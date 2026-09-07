package com.appworks.portal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerMetricEnabledRequest {

    @NotNull(message = "enabled is required")
    private Boolean enabled;
}
