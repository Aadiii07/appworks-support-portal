package com.appworks.portal.dto;

import com.appworks.portal.entity.CustomerMetric;
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
public class CustomerMetricResponse {

    private Long id;
    private Long customerId;
    private MetricResponse metric;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerMetricResponse fromEntity(CustomerMetric cm) {
        return CustomerMetricResponse.builder()
                .id(cm.getId())
                .customerId(cm.getCustomer().getId())
                .metric(MetricResponse.fromEntity(cm.getMetric()))
                .enabled(cm.isEnabled())
                .createdAt(cm.getCreatedAt())
                .updatedAt(cm.getUpdatedAt())
                .build();
    }
}
