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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

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

    private Long createMetric(String serviceKey) throws Exception {
        MetricRequest request = new MetricRequest();
        request.setName("Metric " + serviceKey);
        request.setServiceKey(serviceKey);

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

    @Test
    void summary_reflectsIncreaseAfterNewCustomerAndRun() throws Exception {
        // Other test classes in this suite are not @Transactional and leave
        // committed rows behind, so the database is not guaranteed empty here.
        // Asserting an absolute totalCustomers==0 would be flaky depending on
        // test execution order — instead, capture a baseline and assert the
        // delta after adding known data.
        String baselineJson = mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long baselineCustomers = objectMapper.readTree(baselineJson).get("totalCustomers").asLong();
        long baselineRunsToday = objectMapper.readTree(baselineJson).get("runsToday").asLong();

        Long customerId = createCustomer("DASH1");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_DASH1");
        assignMetricToCustomer(customerId, metricId);

        RunRequest runRequest = new RunRequest();
        runRequest.setCustomerId(customerId);
        runRequest.setEnvironmentId(environmentId);
        runRequest.setMetricIds(Arrays.asList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(runRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCustomers").value(baselineCustomers + 1))
                .andExpect(jsonPath("$.runsToday").value(baselineRunsToday + 1))
                .andExpect(jsonPath("$.successRateToday").exists());
    }

    @Test
    void customerHealth_withNoRuns_returnsNullHealthPercentage() throws Exception {
        Long customerId = createCustomer("DASH2");

        String response = mockMvc.perform(get("/api/v1/dashboard/customer-health"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Find our specific customer in the list rather than assuming array
        // position — other test classes leave committed customers behind, so
        // index [0] is not reliably "the customer we just created."
        com.fasterxml.jackson.databind.JsonNode match = findByCustomerId(response, customerId);
        org.junit.jupiter.api.Assertions.assertNotNull(match, "Expected customer " + customerId + " in health list");
        org.junit.jupiter.api.Assertions.assertEquals(0, match.get("totalRuns").asLong());
        org.junit.jupiter.api.Assertions.assertTrue(
                match.get("healthPercentage") == null || match.get("healthPercentage").isNull());
    }

    @Test
    void customerHealth_afterRuns_calculatesPercentageCorrectly() throws Exception {
        Long customerId = createCustomer("DASH3");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_DASH3");
        assignMetricToCustomer(customerId, metricId);

        RunRequest runRequest = new RunRequest();
        runRequest.setCustomerId(customerId);
        runRequest.setEnvironmentId(environmentId);
        runRequest.setMetricIds(Arrays.asList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(runRequest)))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(get("/api/v1/dashboard/customer-health"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode match = findByCustomerId(response, customerId);
        org.junit.jupiter.api.Assertions.assertNotNull(match, "Expected customer " + customerId + " in health list");
        org.junit.jupiter.api.Assertions.assertEquals(1, match.get("totalRuns").asLong());
        org.junit.jupiter.api.Assertions.assertNotNull(match.get("healthPercentage"));
        org.junit.jupiter.api.Assertions.assertFalse(match.get("healthPercentage").isNull());
    }

    private com.fasterxml.jackson.databind.JsonNode findByCustomerId(String jsonArray, Long customerId) throws Exception {
        com.fasterxml.jackson.databind.JsonNode array = objectMapper.readTree(jsonArray);
        for (com.fasterxml.jackson.databind.JsonNode node : array) {
            if (node.get("customerId").asLong() == customerId) {
                return node;
            }
        }
        return null;
    }

    @Test
    void metricStatus_forNeverRunMetric_returnsNullStatus() throws Exception {
        Long metricId = createMetric("SVC_DASHSTATUS1");

        String response = mockMvc.perform(get("/api/v1/dashboard/metric-status"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode match = findByMetricId(response, metricId);
        org.junit.jupiter.api.Assertions.assertNotNull(match, "Expected metric " + metricId + " in status list");
        org.junit.jupiter.api.Assertions.assertTrue(
                match.get("lastStatus") == null || match.get("lastStatus").isNull());
    }

    @Test
    void metricStatus_afterRun_reflectsTheMostRecentRunStatus() throws Exception {
        Long customerId = createCustomer("DASHSTATUS2");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_DASHSTATUS2");
        assignMetricToCustomer(customerId, metricId);

        RunRequest runRequest = new RunRequest();
        runRequest.setCustomerId(customerId);
        runRequest.setEnvironmentId(environmentId);
        runRequest.setMetricIds(Arrays.asList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(runRequest)))
                .andExpect(status().isCreated());

        String response = mockMvc.perform(get("/api/v1/dashboard/metric-status"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode match = findByMetricId(response, metricId);
        org.junit.jupiter.api.Assertions.assertNotNull(match, "Expected metric " + metricId + " in status list");
        org.junit.jupiter.api.Assertions.assertNotNull(match.get("lastStatus"));
        org.junit.jupiter.api.Assertions.assertFalse(match.get("lastStatus").isNull());
        org.junit.jupiter.api.Assertions.assertNotNull(match.get("lastRunAt"));
    }

    private com.fasterxml.jackson.databind.JsonNode findByMetricId(String jsonArray, Long metricId) throws Exception {
        com.fasterxml.jackson.databind.JsonNode array = objectMapper.readTree(jsonArray);
        for (com.fasterxml.jackson.databind.JsonNode node : array) {
            if (node.get("metricId").asLong() == metricId) {
                return node;
            }
        }
        return null;
    }
}
