package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.appworks.portal.dto.CustomerMetricRequest;
import com.appworks.portal.dto.CustomerRequest;
import com.appworks.portal.dto.EnvironmentRequest;
import com.appworks.portal.dto.MetricRequest;
import com.appworks.portal.dto.RunRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @Transactional from the start this time — the earlier RunControllerTest bug
 * (asserting an exact count against a shared, non-isolated database) is the
 * mistake being deliberately avoided here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RunHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createCustomer(String code) throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("Customer " + code);
        request.setCode(code);
        request.setContactEmail("ops@" + code.toLowerCase() + ".com");

        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createEnvironment(Long customerId, String name) throws Exception {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setName(name);

        String response = mockMvc.perform(post("/api/v1/customers/" + customerId + "/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createMetric(String serviceKey, Double warning, Double critical) throws Exception {
        MetricRequest request = new MetricRequest();
        request.setName("Metric " + serviceKey);
        request.setServiceKey(serviceKey);
        request.setWarningThreshold(warning);
        request.setCriticalThreshold(critical);
        if (warning != null || critical != null) {
            request.setComparisonOperator(com.appworks.portal.entity.ComparisonOperator.GREATER_THAN);
        }

        String response = mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void assignMetricToCustomer(Long customerId, Long metricId) throws Exception {
        CustomerMetricRequest request = new CustomerMetricRequest();
        request.setMetricId(metricId);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private void runMetric(Long customerId, Long environmentId, Long metricId) throws Exception {
        RunRequest runRequest = new RunRequest();
        runRequest.setCustomerId(customerId);
        runRequest.setEnvironmentId(environmentId);
        runRequest.setMetricIds(Collections.singletonList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(runRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void history_filterByCustomerId_returnsOnlyThatCustomersRuns() throws Exception {
        Long customerA = createCustomer("HIST1A");
        Long envA = createEnvironment(customerA, "PROD");
        Long metricA = createMetric("SVC_HIST1A", null, null);
        assignMetricToCustomer(customerA, metricA);
        runMetric(customerA, envA, metricA);

        Long customerB = createCustomer("HIST1B");
        Long envB = createEnvironment(customerB, "PROD");
        Long metricB = createMetric("SVC_HIST1B", null, null);
        assignMetricToCustomer(customerB, metricB);
        runMetric(customerB, envB, metricB);

        mockMvc.perform(get("/api/v1/runs/history").param("customerId", customerA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(customerA));
    }

    @Test
    void history_filterByStatus_returnsOnlyMatchingStatus() throws Exception {
        Long customerId = createCustomer("HIST2");
        Long environmentId = createEnvironment(customerId, "PROD");

        // Metric with no thresholds always evaluates to PASS
        Long passMetric = createMetric("SVC_HIST2_PASS", null, null);
        assignMetricToCustomer(customerId, passMetric);
        runMetric(customerId, environmentId, passMetric);

        // Metric with an extremely low threshold will almost certainly FAIL
        // against the mock client's generated value (0-99.99 range)
        Long failMetric = createMetric("SVC_HIST2_FAIL", 0.01, 0.01);
        assignMetricToCustomer(customerId, failMetric);
        runMetric(customerId, environmentId, failMetric);

        String response = mockMvc.perform(get("/api/v1/runs/history")
                        .param("customerId", customerId.toString())
                        .param("status", "PASS"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
        for (com.fasterxml.jackson.databind.JsonNode run : json.get("content")) {
            org.junit.jupiter.api.Assertions.assertEquals("PASS", run.get("status").asText());
        }
    }

    @Test
    void history_filterByEnvironmentAndMetric_narrowsCorrectly() throws Exception {
        Long customerId = createCustomer("HIST3");
        Long prodEnv = createEnvironment(customerId, "PROD");
        Long stagingEnv = createEnvironment(customerId, "STAGING");
        Long metricId = createMetric("SVC_HIST3", null, null);
        assignMetricToCustomer(customerId, metricId);

        runMetric(customerId, prodEnv, metricId);
        runMetric(customerId, stagingEnv, metricId);

        mockMvc.perform(get("/api/v1/runs/history")
                        .param("customerId", customerId.toString())
                        .param("environmentId", prodEnv.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].environmentId").value(prodEnv));
    }

    @Test
    void history_pagination_respectsPageAndSize() throws Exception {
        Long customerId = createCustomer("HIST4");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_HIST4", null, null);
        assignMetricToCustomer(customerId, metricId);

        // Run 5 times
        for (int i = 0; i < 5; i++) {
            runMetric(customerId, environmentId, metricId);
        }

        mockMvc.perform(get("/api/v1/runs/history")
                        .param("customerId", customerId.toString())
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void history_withNoFilters_returnsAllRunsSharedAcrossSuite() throws Exception {
        Long customerId = createCustomer("HIST5");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_HIST5", null, null);
        assignMetricToCustomer(customerId, metricId);
        runMetric(customerId, environmentId, metricId);

        // No filters at all should still succeed and include our run somewhere
        // in the (potentially large, shared-database) result set.
        String response = mockMvc.perform(get("/api/v1/runs/history").param("size", "1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
        boolean found = false;
        for (com.fasterxml.jackson.databind.JsonNode run : json.get("content")) {
            if (run.get("customerId").asLong() == customerId) {
                found = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(found, "Expected to find our run in the unfiltered history");
    }
}
