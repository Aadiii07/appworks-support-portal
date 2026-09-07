package com.appworks.portal.dto;

import com.appworks.portal.entity.ComparisonOperator;
import com.appworks.portal.entity.Metric;
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
public class MetricResponse {

    private Long id;
    private String name;
    private String serviceKey;
    private String description;
    private String endpoint;
    private boolean enabled;
    private Double warningThreshold;
    private Double criticalThreshold;
    private ComparisonOperator comparisonOperator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MetricResponse fromEntity(Metric metric) {
        return MetricResponse.builder()
                .id(metric.getId())
                .name(metric.getName())
                .serviceKey(metric.getServiceKey())
                .description(metric.getDescription())
                .endpoint(metric.getEndpoint())
                .enabled(metric.isEnabled())
                .warningThreshold(metric.getWarningThreshold())
                .criticalThreshold(metric.getCriticalThreshold())
                .comparisonOperator(metric.getComparisonOperator())
                .createdAt(metric.getCreatedAt())
                .updatedAt(metric.getUpdatedAt())
                .build();
    }
}
