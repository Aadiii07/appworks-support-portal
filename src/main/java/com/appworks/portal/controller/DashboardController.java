package com.appworks.portal.controller;

import com.appworks.portal.dto.CustomerHealthResponse;
import com.appworks.portal.dto.DashboardSummaryResponse;
import com.appworks.portal.dto.MetricStatusResponse;
import com.appworks.portal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/customer-health")
    public List<CustomerHealthResponse> getCustomerHealth() {
        return dashboardService.getCustomerHealth();
    }

    @GetMapping("/metric-status")
    public List<MetricStatusResponse> getMetricStatuses() {
        return dashboardService.getMetricStatuses();
    }
}
