package com.appworks.portal.service;

import com.appworks.portal.dto.CustomerRequest;
import com.appworks.portal.dto.CustomerResponse;
import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.CustomerStatus;
import com.appworks.portal.exception.DuplicateResourceException;
import com.appworks.portal.exception.ResourceNotFoundException;
import com.appworks.portal.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Customer with code '" + request.getCode() + "' already exists");
        }

        Customer customer = Customer.builder()
                .name(request.getName())
                .code(request.getCode())
                .region(request.getRegion())
                .contactEmail(request.getContactEmail())
                .status(request.getStatus() != null ? request.getStatus() : CustomerStatus.ACTIVE)
                .build();

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        return CustomerResponse.fromEntity(getEntityOrThrow(id));
    }

    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getEntityOrThrow(id);

        // If the code is being changed, make sure it doesn't collide with another customer
        if (!customer.getCode().equals(request.getCode())
                && customerRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Customer with code '" + request.getCode() + "' already exists");
        }

        customer.setName(request.getName());
        customer.setCode(request.getCode());
        customer.setRegion(request.getRegion());
        customer.setContactEmail(request.getContactEmail());
        if (request.getStatus() != null) {
            customer.setStatus(request.getStatus());
        }

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    /**
     * Soft delete: sets status to INACTIVE rather than removing the row.
     * Chosen deliberately over a hard delete because Environment, Schedule and Run
     * records reference customer_id — a hard delete would either violate FK
     * constraints or cascade-destroy run history that the dashboard/reporting
     * depend on. Inactive customers are excluded from active-customer counts
     * but their history is preserved.
     */
    public void delete(Long id) {
        Customer customer = getEntityOrThrow(id);
        customer.setStatus(CustomerStatus.INACTIVE);
        customerRepository.save(customer);
    }

    private Customer getEntityOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }
}
