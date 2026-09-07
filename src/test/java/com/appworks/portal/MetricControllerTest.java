package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class MetricControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createMetric_thenFetchIt_succeeds() throws Exception {
        MetricRequest request = new MetricRequest();
        request.setName("Queue Depth Monitor");
        request.setServiceKey("SVC_QUEUE_03");
        request.setDescription("Monitors AppWorks queue depth");
        request.setEndpoint("/api/v1/metrics/queue/03");
        request.setWarningThreshold(300.0);
        request.setCriticalThreshold(500.0);
        request.setComparisonOperator(com.appworks.portal.entity.ComparisonOperator.GREATER_THAN);

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceKey").value("SVC_QUEUE_03"))
                .andExpect(jsonPath("$.criticalThreshold").value(500.0))
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(get("/api/v1/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    void createMetric_withDuplicateServiceKey_returns409() throws Exception {
        MetricRequest request = new MetricRequest();
        request.setName("System Health Check");
        request.setServiceKey("SVC_HEALTH_01");

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createMetric_withMissingName_returns400() throws Exception {
        MetricRequest request = new MetricRequest();
        request.setServiceKey("SVC_NONAME_01");

        mockMvc.perform(post("/api/v1/metrics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
