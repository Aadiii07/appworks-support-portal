package com.appworks.portal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvironmentRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String baseUrl;

    /** Optional. Defaults to true (enabled) on create if not supplied. */
    private Boolean enabled;
}
