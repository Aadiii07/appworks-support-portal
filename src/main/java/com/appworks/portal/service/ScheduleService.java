package com.appworks.portal.service;

import com.appworks.portal.dto.ScheduleRequest;
import com.appworks.portal.dto.ScheduleResponse;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.entity.Schedule;
import com.appworks.portal.exception.BadRequestException;
import com.appworks.portal.exception.DuplicateResourceException;
import com.appworks.portal.exception.ResourceNotFoundException;
import com.appworks.portal.repository.CustomerMetricRepository;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.repository.EnvironmentRepository;
import com.appworks.portal.repository.MetricRepository;
import com.appworks.portal.repository.RunRepository;
import com.appworks.portal.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CustomerRepository customerRepository;
    private final EnvironmentRepository environmentRepository;
    private final MetricRepository metricRepository;
    private final CustomerMetricRepository customerMetricRepository;
    private final RunRepository runRepository;

    public ScheduleResponse create(ScheduleRequest request) {
        Customer customer = getCustomerOrThrow(request.getCustomerId());
        Environment environment = getEnvironmentOrThrow(request.getEnvironmentId());
        Metric metric = getMetricOrThrow(request.getMetricId());

        validateEnvironmentBelongsToCustomer(environment, customer);
        validateMetricAssignedToCustomer(customer.getId(), metric.getId());
        String normalizedCron = normalize(request.getCronExpression());
        validateCron(normalizedCron);

        if (scheduleRepository.existsByCustomerIdAndEnvironmentIdAndMetricId(
                customer.getId(), environment.getId(), metric.getId())) {
            throw new DuplicateResourceException(
                    "A schedule already exists for this customer/environment/metric combination");
        }

        Schedule schedule = Schedule.builder()
                .customer(customer)
                .environment(environment)
                .metric(metric)
                .cronExpression(normalizedCron)
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAll(Long customerIdFilter) {
        List<Schedule> schedules = customerIdFilter != null
                ? scheduleRepository.findByCustomerId(customerIdFilter)
                : scheduleRepository.findAll();
        return schedules.stream().map(ScheduleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse findById(Long id) {
        return ScheduleResponse.from(getScheduleOrThrow(id));
    }

    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule schedule = getScheduleOrThrow(id);
        Customer customer = getCustomerOrThrow(request.getCustomerId());
        Environment environment = getEnvironmentOrThrow(request.getEnvironmentId());
        Metric metric = getMetricOrThrow(request.getMetricId());

        validateEnvironmentBelongsToCustomer(environment, customer);
        validateMetricAssignedToCustomer(customer.getId(), metric.getId());
        String normalizedCron = normalize(request.getCronExpression());
        validateCron(normalizedCron);

        boolean tripleChanged = !schedule.getCustomer().getId().equals(customer.getId())
                || !schedule.getEnvironment().getId().equals(environment.getId())
                || !schedule.getMetric().getId().equals(metric.getId());
        if (tripleChanged && scheduleRepository.existsByCustomerIdAndEnvironmentIdAndMetricIdAndIdNot(
                customer.getId(), environment.getId(), metric.getId(), id)) {
            throw new DuplicateResourceException(
                    "A schedule already exists for this customer/environment/metric combination");
        }

        schedule.setCustomer(customer);
        schedule.setEnvironment(environment);
        schedule.setMetric(metric);
        schedule.setCronExpression(normalizedCron);
        if (request.getEnabled() != null) {
            schedule.setEnabled(request.getEnabled());
        }

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public void delete(Long id) {
        Schedule schedule = getScheduleOrThrow(id);
        // Detach any Run rows pointing at this schedule first — otherwise the
        // delete below throws a raw foreign-key violation if the schedule has
        // ever executed. Detaching preserves those runs in Run History; only
        // the back-reference to this now-deleted schedule is cleared.
        runRepository.detachFromSchedule(id);
        scheduleRepository.delete(schedule);
    }

    /**
     * The reference UI's cron examples are 5-field Unix format (minute hour day
     * month weekday, e.g. "*&#47;5 * * * *"). Spring's CronExpression/@Scheduled
     * require 6 fields with seconds first. We accept 5-field input and store the
     * normalized 6-field form so the scheduler can use it directly.
     */
    static String normalize(String cronExpression) {
        String trimmed = cronExpression.trim().replaceAll("\\s+", " ");
        return trimmed.split(" ").length == 5 ? "0 " + trimmed : trimmed;
    }

    private void validateCron(String normalizedCron) {
        if (!CronExpression.isValidExpression(normalizedCron)) {
            throw new BadRequestException(
                    "Invalid cron expression. Use 5-field Unix format (e.g. '*/5 * * * *') "
                            + "or 6-field Spring format with seconds (e.g. '0 */5 * * * *').");
        }
    }

    private void validateEnvironmentBelongsToCustomer(Environment environment, Customer customer) {
        if (!environment.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Environment does not belong to the specified customer");
        }
    }

    /**
     * This check was missing before this audit — the previous ScheduleService let
     * you schedule any metric that exists globally, without confirming it was
     * ever assigned to the customer via CustomerMetric. That defeats the point
     * of the assignment step.
     */
    private void validateMetricAssignedToCustomer(Long customerId, Long metricId) {
        if (!customerMetricRepository.existsByCustomerIdAndMetricId(customerId, metricId)) {
            throw new BadRequestException(
                    "Metric " + metricId + " is not assigned to customer " + customerId
                            + " — assign it via POST /api/v1/customers/" + customerId
                            + "/metrics before scheduling it");
        }
    }

    private Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private Environment getEnvironmentOrThrow(Long environmentId) {
        return environmentRepository.findById(environmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id " + environmentId));
    }

    private Metric getMetricOrThrow(Long metricId) {
        return metricRepository.findById(metricId)
                .orElseThrow(() -> new ResourceNotFoundException("Metric not found with id " + metricId));
    }

    private Schedule getScheduleOrThrow(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id " + id));
    }
}
