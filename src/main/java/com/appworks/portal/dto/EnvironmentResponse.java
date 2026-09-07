package com.appworks.portal.dto;

import com.appworks.portal.entity.Environment;
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
public class EnvironmentResponse {

    private Long id;
    private Long customerId;
    private String name;
    private String baseUrl;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EnvironmentResponse fromEntity(Environment env) {
        return EnvironmentResponse.builder()
                .id(env.getId())
                .customerId(env.getCustomer().getId())
                .name(env.getName())
                .baseUrl(env.getBaseUrl())
                .enabled(env.isEnabled())
                .createdAt(env.getCreatedAt())
                .updatedAt(env.getUpdatedAt())
                .build();
    }
}
