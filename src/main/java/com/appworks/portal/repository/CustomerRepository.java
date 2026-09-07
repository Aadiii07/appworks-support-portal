package com.appworks.portal.repository;

import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.CustomerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCode(String code);

    boolean existsByCode(String code);

    long countByStatus(CustomerStatus status);
}
