package com.appworks.portal.repository;

import com.appworks.portal.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    List<Environment> findByCustomerId(Long customerId);

    Optional<Environment> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByCustomerIdAndNameIgnoreCase(Long customerId, String name);
}
