package com.appworks.portal.dto;

import com.appworks.portal.entity.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "code is required")
    private String code;

    private String region;

    @NotBlank(message = "contactEmail is required")
    @Email(message = "contactEmail must be a valid email address")
    private String contactEmail;

    /**
     * Optional. Defaults to ACTIVE on create if not supplied.
     */
    private CustomerStatus status;
}
