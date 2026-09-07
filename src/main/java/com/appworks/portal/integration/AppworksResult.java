package com.appworks.portal.integration;

/**
 * The result of one AppWorks invocation: a numeric value (compared against the
 * metric's thresholds by MonitoringService) plus a human-readable output string
 * (stored on the Run record for display in Run History).
 */
public record AppworksResult(Double value, String output) {
}
