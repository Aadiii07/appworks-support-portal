package com.appworks.portal.dto;

import com.appworks.portal.entity.Customer;
import com.appworks.portal.entity.CustomerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;
    private String name;
    private String code;
    private String region;
    private String contactEmail;
    private CustomerStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerResponse fromEntity(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .code(customer.getCode())
                .region(customer.getRegion())
                .contactEmail(customer.getContactEmail())
                .status(customer.getStatus())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
