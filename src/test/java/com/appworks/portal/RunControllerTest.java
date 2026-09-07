package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.appworks.portal.dto.CustomerRequest;
import com.appworks.portal.dto.EnvironmentRequest;
import com.appworks.portal.dto.MetricRequest;
import com.appworks.portal.dto.CustomerMetricRequest;
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
class RunControllerTest {

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
        request.setBaseUrl("https://env-" + name.toLowerCase() + ".example.com/api");

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
        request.setWarningThreshold(50.0);
        request.setCriticalThreshold(100.0);

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
    void runNow_withValidMetrics_succeeds() throws Exception {
        Long customerId = createCustomer("RUNTEST1");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_RUNTEST_01");
        assignMetricToCustomer(customerId, metricId);

        RunRequest request = new RunRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricIds(Arrays.asList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("PASS"))
                .andExpect(jsonPath("$[0].source").value("MANUAL"))
                .andExpect(jsonPath("$[0].durationMs").isNumber());
    }

    @Test
    void runNow_withMultipleMetrics_runsAll() throws Exception {
        Long customerId = createCustomer("RUNTEST2");
        Long environmentId = createEnvironment(customerId, "STAGING");
        Long metric1Id = createMetric("SVC_RUNTEST_02A");
        Long metric2Id = createMetric("SVC_RUNTEST_02B");
        assignMetricToCustomer(customerId, metric1Id);
        assignMetricToCustomer(customerId, metric2Id);

        RunRequest request = new RunRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricIds(Arrays.asList(metric1Id, metric2Id));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void runNow_withUnassignedMetric_returns400() throws Exception {
        Long customerId = createCustomer("RUNTEST3");
        Long environmentId = createEnvironment(customerId, "DEV");
        Long metricId = createMetric("SVC_RUNTEST_03");
        // NOT assigned

        RunRequest request = new RunRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricIds(Arrays.asList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runNow_withWrongEnvironmentCustomer_returns400() throws Exception {
        Long customerId1 = createCustomer("RUNTEST4A");
        Long customerId2 = createCustomer("RUNTEST4B");
        Long environment2Id = createEnvironment(customerId2, "PROD");
        Long metricId = createMetric("SVC_RUNTEST_04");
        assignMetricToCustomer(customerId1, metricId);

        RunRequest request = new RunRequest();
        request.setCustomerId(customerId1);
        request.setEnvironmentId(environment2Id); // belongs to customer 2
        request.setMetricIds(Arrays.asList(metricId));

        mockMvc.perform(post("/api/v1/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recentHistory_returnsLatestRuns() throws Exception {
        Long customerId = createCustomer("RUNTEST5");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_RUNTEST_05");
        assignMetricToCustomer(customerId, metricId);

        RunRequest runRequest = new RunRequest();
        runRequest.setCustomerId(customerId);
        runRequest.setEnvironmentId(environmentId);
        runRequest.setMetricIds(Arrays.asList(metricId));

        // Execute 3 times
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(runRequest)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
