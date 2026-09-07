package com.appworks.portal.service;

import com.appworks.portal.dto.EnvironmentRequest;
import com.appworks.portal.dto.EnvironmentResponse;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.Environment;
import com.appworks.portal.exception.DuplicateResourceException;
import com.appworks.portal.exception.ResourceNotFoundException;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.repository.EnvironmentRepository;
import com.appworks.portal.repository.ScheduleRepository;
import com.appworks.portal.exception.ResourceInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final CustomerRepository customerRepository;
    private final ScheduleRepository scheduleRepository;

    public EnvironmentResponse create(Long customerId, EnvironmentRequest request) {
        Customer customer = getCustomerOrThrow(customerId);

        if (environmentRepository.existsByCustomerIdAndNameIgnoreCase(customerId, request.getName())) {
            throw new DuplicateResourceException(
                    "Environment '" + request.getName() + "' already exists for this customer");
        }

        Environment environment = Environment.builder()
                .customer(customer)
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();

        Environment saved = environmentRepository.save(environment);
        return EnvironmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> findAllForCustomer(Long customerId) {
        getCustomerOrThrow(customerId); // 404 if the customer itself doesn't exist
        return environmentRepository.findByCustomerId(customerId).stream()
                .map(EnvironmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse findOne(Long customerId, Long environmentId) {
        return EnvironmentResponse.fromEntity(getEntityOrThrow(customerId, environmentId));
    }

    public EnvironmentResponse update(Long customerId, Long environmentId, EnvironmentRequest request) {
        Environment environment = getEntityOrThrow(customerId, environmentId);

        if (!environment.getName().equalsIgnoreCase(request.getName())
                && environmentRepository.existsByCustomerIdAndNameIgnoreCase(customerId, request.getName())) {
            throw new DuplicateResourceException(
                    "Environment '" + request.getName() + "' already exists for this customer");
        }

        environment.setName(request.getName());
        environment.setBaseUrl(request.getBaseUrl());
        if (request.getEnabled() != null) {
            environment.setEnabled(request.getEnabled());
        }

        Environment saved = environmentRepository.save(environment);
        return EnvironmentResponse.fromEntity(saved);
    }

    /**
     * Blocked if any Schedule still references this environment — otherwise the
     * database's FK constraint would throw a raw, unhandled error. This guard
     * was missing before this audit even though ScheduleRepository already had
     * the existsByEnvironmentId method sitting unused.
     */
    public void delete(Long customerId, Long environmentId) {
        Environment environment = getEntityOrThrow(customerId, environmentId);
        if (scheduleRepository.existsByEnvironmentId(environmentId)) {
            throw new ResourceInUseException(
                    "Cannot delete environment '" + environment.getName()
                            + "': it is still referenced by one or more schedules. Delete those first.");
        }
        environmentRepository.delete(environment);
    }

    private Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private Environment getEntityOrThrow(Long customerId, Long environmentId) {
        return environmentRepository.findByIdAndCustomerId(environmentId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Environment not found with id " + environmentId + " for customer " + customerId));
    }
}
