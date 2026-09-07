package com.appworks.portal.repository;

import com.appworks.portal.entity.CustomerMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerMetricRepository extends JpaRepository<CustomerMetric, Long> {

    List<CustomerMetric> findByCustomerId(Long customerId);

    Optional<CustomerMetric> findByIdAndCustomerId(Long id, Long customerId);

    Optional<CustomerMetric> findByCustomerIdAndMetricId(Long customerId, Long metricId);

    boolean existsByCustomerIdAndMetricId(Long customerId, Long metricId);

    boolean existsByMetricId(Long metricId);

    /** Used by the Dashboard's "metrics configured" count. */
    long countByEnabledTrue();
}
