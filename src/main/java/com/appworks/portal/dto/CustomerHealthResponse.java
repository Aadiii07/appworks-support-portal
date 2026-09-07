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
public class CustomerHealthResponse {

    private Long customerId;
    private String customerName;
    private long totalRuns;
    private long passRuns;

    /** Percentage of all-time runs that were PASS. Null if the customer has no
     * runs yet, rather than showing a misleading 0% or 100%. */
    private Double healthPercentage;
}
