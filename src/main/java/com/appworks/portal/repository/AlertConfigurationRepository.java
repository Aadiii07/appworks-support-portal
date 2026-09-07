package com.appworks.portal.repository;

import com.appworks.portal.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Long> {
    Optional<AlertConfiguration> findByCustomerId(Long customerId);
}
