package com.appworks.portal.dto;

import com.appworks.portal.entity.ComparisonOperator;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetricRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "serviceKey is required")
    private String serviceKey;

    private String description;

    private String endpoint;

    /** Optional. Defaults to true on create if not supplied. */
    private Boolean enabled;

    /** Optional threshold config — omit all three if this metric isn't numerically evaluated. */
    private Double warningThreshold;
    private Double criticalThreshold;
    private ComparisonOperator comparisonOperator;
}
