package com.appworks.portal.repository;

import com.appworks.portal.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricRepository extends JpaRepository<Metric, Long> {

    boolean existsByServiceKeyIgnoreCase(String serviceKey);

    boolean existsByServiceKeyIgnoreCaseAndIdNot(String serviceKey, Long id);
}
