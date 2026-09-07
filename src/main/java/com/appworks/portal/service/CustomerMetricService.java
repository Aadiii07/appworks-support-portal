package com.appworks.portal.service;

import com.appworks.portal.dto.CustomerMetricRequest;
import com.appworks.portal.dto.CustomerMetricResponse;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.CustomerMetric;
import com.appworks.portal.entity.Metric;
import com.appworks.portal.exception.DuplicateResourceException;
import com.appworks.portal.exception.ResourceNotFoundException;
import com.appworks.portal.repository.CustomerMetricRepository;
import com.appworks.portal.repository.CustomerRepository;
import com.appworks.portal.repository.MetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerMetricService {

    private final CustomerMetricRepository customerMetricRepository;
    private final CustomerRepository customerRepository;
    private final MetricRepository metricRepository;

    public CustomerMetricResponse assign(Long customerId, CustomerMetricRequest request) {
        Customer customer = getCustomerOrThrow(customerId);
        Metric metric = metricRepository.findById(request.getMetricId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Metric not found with id " + request.getMetricId()));

        if (customerMetricRepository.existsByCustomerIdAndMetricId(customerId, metric.getId())) {
            throw new DuplicateResourceException(
                    "Metric '" + metric.getServiceKey() + "' is already assigned to this customer");
        }

        CustomerMetric customerMetric = CustomerMetric.builder()
                .customer(customer)
                .metric(metric)
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();

        CustomerMetric saved = customerMetricRepository.save(customerMetric);
        return CustomerMetricResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerMetricResponse> findAllForCustomer(Long customerId) {
        getCustomerOrThrow(customerId);
        return customerMetricRepository.findByCustomerId(customerId).stream()
                .map(CustomerMetricResponse::fromEntity)
                .toList();
    }

    public CustomerMetricResponse updateEnabled(Long customerId, Long customerMetricId, boolean enabled) {
        CustomerMetric customerMetric = getEntityOrThrow(customerId, customerMetricId);
        customerMetric.setEnabled(enabled);
        CustomerMetric saved = customerMetricRepository.save(customerMetric);
        return CustomerMetricResponse.fromEntity(saved);
    }

    public void unassign(Long customerId, Long customerMetricId) {
        CustomerMetric customerMetric = getEntityOrThrow(customerId, customerMetricId);
        customerMetricRepository.delete(customerMetric);
    }

    private Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private CustomerMetric getEntityOrThrow(Long customerId, Long customerMetricId) {
        return customerMetricRepository.findByIdAndCustomerId(customerMetricId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer-metric assignment not found with id " + customerMetricId
                                + " for customer " + customerId));
    }
}
