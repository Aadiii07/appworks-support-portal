package com.appworks.portal.dto;

import com.appworks.portal.entity.Run;
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
public class RunResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long environmentId;
    private String environmentName;
    private Long metricId;
    private String metricName;
    private String serviceKey;
    private Long scheduleId;
    private String status;
    private String source;
    private String output;
    private String errorMessage;
    private Double rawValue;
    private long durationMs;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static RunResponse from(Run run) {
        return RunResponse.builder()
                .id(run.getId())
                .customerId(run.getCustomer().getId())
                .customerName(run.getCustomer().getName())
                .environmentId(run.getEnvironment().getId())
                .environmentName(run.getEnvironment().getName())
                .metricId(run.getMetric().getId())
                .metricName(run.getMetric().getName())
                .serviceKey(run.getMetric().getServiceKey())
                .scheduleId(run.getSchedule() == null ? null : run.getSchedule().getId())
                .status(run.getStatus().name())
                .source(run.getSource().name())
                .output(run.getOutput())
                .errorMessage(run.getErrorMessage())
                .rawValue(run.getRawValue())
                .durationMs(run.getDurationMs())
                .startedAt(run.getStartedAt())
                .completedAt(run.getCompletedAt())
                .build();
    }
}
