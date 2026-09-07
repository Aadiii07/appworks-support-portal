package com.appworks.portal.repository;

import com.appworks.portal.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByCustomerId(Long customerId);

    Optional<Schedule> findByIdAndCustomerId(Long id, Long customerId);

    /** Used by SchedulePollingService (Milestone 5) to find schedules due to run. */
    List<Schedule> findByEnabledTrue();

    boolean existsByCustomerIdAndEnvironmentIdAndMetricId(Long customerId, Long environmentId, Long metricId);

    boolean existsByCustomerIdAndEnvironmentIdAndMetricIdAndIdNot(
            Long customerId, Long environmentId, Long metricId, Long id);

    /** Used by EnvironmentService.delete() to block deleting an environment still in use. */
    boolean existsByEnvironmentId(Long environmentId);

    /** Used by MetricService.delete() to block deleting a metric still in use. */
    boolean existsByMetricId(Long metricId);
}
