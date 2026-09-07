package com.appworks.portal.service;

import com.appworks.portal.dto.RunRequest;
import com.appworks.portal.dto.RunResponse;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunSource;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.entity.Schedule;
import com.appworks.portal.exception.BadRequestException;
import com.appworks.portal.exception.ResourceNotFoundException;
import com.appworks.portal.integration.AppworksClient;
import com.appworks.portal.integration.AppworksResult;
import com.appworks.portal.repository.CustomerMetricRepository;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.repository.EnvironmentRepository;
import com.appworks.portal.repository.MetricRepository;
import com.appworks.portal.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The core monitoring engine: given a Customer + Environment + Metric, invokes
 * AppWorks (via the injected client, currently MockAppworksClient), evaluates the
 * result against the metric's thresholds, saves a Run record, and — this was
 * missing before this audit — actually notifies AlertService so alerts have a
 * chance to fire. Handles both scheduled execution (from SchedulePollingService)
 * and manual execution (from RunController's "Run Now").
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MonitoringService {

    private final RunRepository runRepository;
    private final CustomerRepository customerRepository;
    private final EnvironmentRepository environmentRepository;
    private final MetricRepository metricRepository;
    private final CustomerMetricRepository customerMetricRepository;
    private final AppworksClient appworksClient;
    private final AlertService alertService;

    public RunResponse executeSchedule(Schedule schedule) {
        RunResponse result = execute(
                schedule.getCustomer(), schedule.getEnvironment(), schedule.getMetric(),
                schedule, RunSource.SCHEDULED);
        schedule.setLastExecutedAt(LocalDateTime.now());
        return result;
    }

    public List<RunResponse> executeManual(RunRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id " + request.getCustomerId()));

        Environment environment = environmentRepository.findById(request.getEnvironmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Environment not found with id " + request.getEnvironmentId()));

        if (!environment.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Environment does not belong to the specified customer");
        }

        return request.getMetricIds().stream()
                .distinct()
                .map(metricId -> {
                    Metric metric = metricRepository.findById(metricId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Metric not found with id " + metricId));
                    if (!customerMetricRepository.existsByCustomerIdAndMetricId(customer.getId(), metricId)) {
                        throw new BadRequestException(
                                "Metric " + metricId + " is not assigned to customer " + customer.getId());
                    }
                    return execute(customer, environment, metric, null, RunSource.MANUAL);
                })
                .toList();
    }

    private RunResponse execute(Customer customer, Environment environment, Metric metric,
                                 Schedule schedule, RunSource source) {
        LocalDateTime start = LocalDateTime.now();
        long startNanos = System.nanoTime();

        Run run;
        try {
            AppworksResult result = appworksClient.execute(customer, environment, metric);
            run = Run.builder()
                    .customer(customer)
                    .environment(environment)
                    .metric(metric)
                    .schedule(schedule)
                    .source(source)
                    .status(evaluate(metric, result.value()))
                    .startedAt(start)
                    .completedAt(LocalDateTime.now())
                    .durationMs(elapsedMs(startNanos))
                    .rawValue(result.value())
                    .output(result.output())
                    .build();
        } catch (Exception ex) {
            run = Run.builder()
                    .customer(customer)
                    .environment(environment)
                    .metric(metric)
                    .schedule(schedule)
                    .source(source)
                    .status(RunStatus.ERROR)
                    .startedAt(start)
                    .completedAt(LocalDateTime.now())
                    .durationMs(elapsedMs(startNanos))
                    .errorMessage(ex.getMessage())
                    .build();
        }

        run = runRepository.save(run);

        // This call was missing before this audit — AlertService existed but was
        // never invoked, so alerts never fired regardless of run outcome.
        alertService.notify(run);

        return RunResponse.from(run);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /**
     * Turns a raw numeric value into PASS / WARNING / FAIL using the metric's
     * configured thresholds. A metric with no thresholds configured, or a null
     * value from AppWorks, is treated as PASS (nothing to compare against).
     */
    private RunStatus evaluate(Metric metric, Double value) {
        if (value == null || metric.getComparisonOperator() == null) {
            return RunStatus.PASS;
        }

        boolean higherIsWorse = metric.getComparisonOperator().name().startsWith("GREATER");

        if (metric.getCriticalThreshold() != null) {
            boolean breachesCritical = higherIsWorse
                    ? value >= metric.getCriticalThreshold()
                    : value <= metric.getCriticalThreshold();
            if (breachesCritical) {
                return RunStatus.FAIL;
            }
        }

        if (metric.getWarningThreshold() != null) {
            boolean breachesWarning = higherIsWorse
                    ? value >= metric.getWarningThreshold()
                    : value <= metric.getWarningThreshold();
            if (breachesWarning) {
                return RunStatus.WARNING;
            }
        }

        return RunStatus.PASS;
    }
}
