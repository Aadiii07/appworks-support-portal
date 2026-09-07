package com.appworks.portal.dto;

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
public class MetricStatusResponse {

    private Long metricId;
    private String metricName;
    private String serviceKey;

    /** Null if this metric has never been run (not "PASS" or any other default). */
    private String lastStatus;
    private LocalDateTime lastRunAt;
}
