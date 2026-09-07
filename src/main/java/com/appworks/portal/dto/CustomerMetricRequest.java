package com.appworks.portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerMetricRequest {

    @NotNull(message = "metricId is required")
    private Long metricId;

    /** Optional. Defaults to true on create if not supplied. */
    private Boolean enabled;
}
