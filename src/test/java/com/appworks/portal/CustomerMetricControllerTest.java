package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.appworks.portal.dto.CustomerMetricEnabledRequest;
import com.appworks.portal.dto.CustomerMetricRequest;
import com.appworks.portal.dto.CustomerRequest;
import com.appworks.portal.dto.MetricRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerMetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createCustomer(String code) throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("Test Customer " + code);
        request.setCode(code);
        request.setContactEmail("ops@" + code.toLowerCase() + ".com");

        String response = mockMvc.perform(post("/api/v1/customers")
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

    @Test
    void assignMetricToCustomer_thenListIt_succeeds() throws Exception {
        Long customerId = createCustomer("CMTEST1");
        Long metricId = createMetric("SVC_CMTEST1_01");

        CustomerMetricRequest assignRequest = new CustomerMetricRequest();
        assignRequest.setMetricId(metricId);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metric.serviceKey").value("SVC_CMTEST1_01"))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/api/v1/customers/" + customerId + "/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void assignSameMetricTwice_returns409() throws Exception {
        Long customerId = createCustomer("CMTEST2");
        Long metricId = createMetric("SVC_CMTEST2_01");

        CustomerMetricRequest assignRequest = new CustomerMetricRequest();
        assignRequest.setMetricId(metricId);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteMetric_stillAssignedToCustomer_returns409() throws Exception {
        Long customerId = createCustomer("CMTEST3");
        Long metricId = createMetric("SVC_CMTEST3_01");

        CustomerMetricRequest assignRequest = new CustomerMetricRequest();
        assignRequest.setMetricId(metricId);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isCreated());

        // Attempting to delete a metric that's still assigned must be rejected cleanly,
        // not with a raw foreign-key-constraint 500 error.
        mockMvc.perform(delete("/api/v1/metrics/" + metricId))
                .andExpect(status().isConflict());
    }

    @Test
    void toggleCustomerMetricEnabled_updatesFlag() throws Exception {
        Long customerId = createCustomer("CMTEST4");
        Long metricId = createMetric("SVC_CMTEST4_01");

        CustomerMetricRequest assignRequest = new CustomerMetricRequest();
        assignRequest.setMetricId(metricId);

        String response = mockMvc.perform(post("/api/v1/customers/" + customerId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andReturn().getResponse().getContentAsString();

        Long customerMetricId = objectMapper.readTree(response).get("id").asLong();

        CustomerMetricEnabledRequest toggleRequest = new CustomerMetricEnabledRequest();
        toggleRequest.setEnabled(false);

        mockMvc.perform(patch("/api/v1/customers/" + customerId + "/metrics/" + customerMetricId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toggleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
