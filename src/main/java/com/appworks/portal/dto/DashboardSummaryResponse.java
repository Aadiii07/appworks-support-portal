package com.appworks.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalCustomers;
    private long activeCustomers;
    private long metricsConfigured;
    private long runsToday;
    private long passToday;
    private long warningToday;
    private long failToday;
    private long errorToday;

    /** Percentage of today's runs that were PASS. Null if there were no runs today
     * (rather than misleadingly showing 0% or 100%). */
    private Double successRateToday;
}
