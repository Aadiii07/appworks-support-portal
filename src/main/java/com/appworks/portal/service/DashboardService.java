package com.appworks.portal.service;

import com.appworks.portal.dto.CustomerHealthResponse;
import com.appworks.portal.dto.DashboardSummaryResponse;
import com.appworks.portal.dto.MetricStatusResponse;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.CustomerStatus;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.Run;
import com.appworks.portal.entity.RunStatus;
import com.appworks.portal.repository.CustomerMetricRepository;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.repository.MetricRepository;
import com.appworks.portal.repository.RunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Every number here is computed live from the database on each request — nothing
 * is cached or hardcoded, per the original requirement that dashboard stats must
 * be real and DB-driven, not the static demo values in the reference HTML.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final CustomerMetricRepository customerMetricRepository;
    private final MetricRepository metricRepository;
    private final RunRepository runRepository;

    public DashboardSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        long runsToday = runRepository.countByStartedAtBetween(startOfToday, startOfTomorrow);
        long passToday = runRepository.countByStatusAndStartedAtBetween(RunStatus.PASS, startOfToday, startOfTomorrow);
        long warningToday = runRepository.countByStatusAndStartedAtBetween(RunStatus.WARNING, startOfToday, startOfTomorrow);
        long failToday = runRepository.countByStatusAndStartedAtBetween(RunStatus.FAIL, startOfToday, startOfTomorrow);
        long errorToday = runRepository.countByStatusAndStartedAtBetween(RunStatus.ERROR, startOfToday, startOfTomorrow);

        return DashboardSummaryResponse.builder()
                .totalCustomers(customerRepository.count())
                .activeCustomers(customerRepository.countByStatus(CustomerStatus.ACTIVE))
                .metricsConfigured(customerMetricRepository.countByEnabledTrue())
                .runsToday(runsToday)
                .passToday(passToday)
                .warningToday(warningToday)
                .failToday(failToday)
                .errorToday(errorToday)
                .successRateToday(percentage(passToday, runsToday))
                .build();
    }

    /**
     * Health is defined as: percentage of a customer's all-time runs that were
     * PASS. WARNING and FAIL both count against health — a customer sitting at
     * "WARNING" isn't fully healthy. This is a design decision, not the only
     * valid one; if you want a rolling window (e.g. last 7 days) instead of
     * all-time, this is where to change it.
     */
    public List<CustomerHealthResponse> getCustomerHealth() {
        List<Customer> customers = customerRepository.findAll();

        return customers.stream()
                .map(customer -> {
                    long totalRuns = runRepository.countByCustomerId(customer.getId());
                    long passRuns = runRepository.countByCustomerIdAndStatus(customer.getId(), RunStatus.PASS);
                    return CustomerHealthResponse.builder()
                            .customerId(customer.getId())
                            .customerName(customer.getName())
                            .totalRuns(totalRuns)
                            .passRuns(passRuns)
                            .healthPercentage(percentage(passRuns, totalRuns))
                            .build();
                })
                .toList();
    }

    private Double percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    /**
     * For the Dashboard's "All Metrics — Last Run Status" grid: every metric
     * in the catalog, with its single most recent run's status (across all
     * customers/environments that run it). A metric that's never been run
     * shows lastStatus = null rather than a misleading default.
     */
    public List<MetricStatusResponse> getMetricStatuses() {
        List<Metric> metrics = metricRepository.findAll();

        return metrics.stream()
                .map(metric -> {
                    Optional<Run> latestRun = runRepository.findTopByMetricIdOrderByStartedAtDesc(metric.getId());
                    return MetricStatusResponse.builder()
                            .metricId(metric.getId())
                            .metricName(metric.getName())
                            .serviceKey(metric.getServiceKey())
                            .lastStatus(latestRun.map(r -> r.getStatus().name()).orElse(null))
                            .lastRunAt(latestRun.map(Run::getStartedAt).orElse(null))
                            .build();
                })
                .toList();
    }
}
