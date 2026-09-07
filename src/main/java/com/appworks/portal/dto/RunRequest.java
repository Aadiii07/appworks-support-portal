package com.appworks.portal.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RunRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "environmentId is required")
    private Long environmentId;

    @NotEmpty(message = "metricIds must contain at least one metric id")
    private List<Long> metricIds;
}
