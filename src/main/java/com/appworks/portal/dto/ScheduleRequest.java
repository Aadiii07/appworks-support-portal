package com.appworks.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "environmentId is required")
    private Long environmentId;

    @NotNull(message = "metricId is required")
    private Long metricId;

    @NotBlank(message = "cronExpression is required")
    private String cronExpression;

    /** Optional. Defaults to true on create if not supplied. */
    private Boolean enabled;
}
