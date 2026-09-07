package com.appworks.portal.dto;

import com.appworks.portal.entity.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long environmentId;
    private String environmentName;
    private Long metricId;
    private String metricName;
    private String cronExpression;
    private boolean enabled;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .customerId(schedule.getCustomer().getId())
                .customerName(schedule.getCustomer().getName())
                .environmentId(schedule.getEnvironment().getId())
                .environmentName(schedule.getEnvironment().getName())
                .metricId(schedule.getMetric().getId())
                .metricName(schedule.getMetric().getName())
                .cronExpression(schedule.getCronExpression())
                .enabled(schedule.isEnabled())
                .lastExecutedAt(schedule.getLastExecutedAt())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
