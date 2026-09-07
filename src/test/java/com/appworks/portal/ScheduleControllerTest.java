package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.appworks.portal.dto.CustomerRequest;
import com.appworks.portal.dto.EnvironmentRequest;
import com.appworks.portal.dto.MetricRequest;
import com.appworks.portal.dto.CustomerMetricRequest;
import com.appworks.portal.dto.ScheduleRequest;
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
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.appworks.portal.repository.ScheduleRepository scheduleRepository;

    @Autowired
    private com.appworks.portal.repository.RunRepository runRepository;

    @Autowired
    private com.appworks.portal.service.MonitoringService monitoringService;

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
        request.setWarningThreshold(100.0);
        request.setCriticalThreshold(200.0);

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
    void createSchedule_withValidReferences_succeeds() throws Exception {
        Long customerId = createCustomer("SCHEDTEST1");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_01");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest request = new ScheduleRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricId(metricId);
        request.setCronExpression("*/5 * * * *");

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cronExpression").value("0 */5 * * * *"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void createSchedule_withUnassignedMetric_returns400() throws Exception {
        Long customerId = createCustomer("SCHEDTEST2");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_02");
        // NOT assigned to customer

        ScheduleRequest request = new ScheduleRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricId(metricId);
        request.setCronExpression("0 * * * *");

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSchedule_withInvalidCron_returns400() throws Exception {
        Long customerId = createCustomer("SCHEDTEST3");
        Long environmentId = createEnvironment(customerId, "STAGING");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_03");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest request = new ScheduleRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricId(metricId);
        request.setCronExpression("not a cron at all");

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSchedule_withDuplicateCustomerEnvironmentMetric_returns409() throws Exception {
        Long customerId = createCustomer("SCHEDTEST4");
        Long environmentId = createEnvironment(customerId, "DEV");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_04");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest request = new ScheduleRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricId(metricId);
        request.setCronExpression("0 * * * *");

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getSchedule_byId_succeeds() throws Exception {
        Long customerId = createCustomer("SCHEDTEST5");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_05");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest createRequest = new ScheduleRequest();
        createRequest.setCustomerId(customerId);
        createRequest.setEnvironmentId(environmentId);
        createRequest.setMetricId(metricId);
        createRequest.setCronExpression("*/15 * * * *");

        String createResponse = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        Long scheduleId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/schedules/" + scheduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scheduleId))
                .andExpect(jsonPath("$.cronExpression").value("0 */15 * * * *"));
    }

    @Test
    void updateSchedule_succeeds() throws Exception {
        Long customerId = createCustomer("SCHEDTEST6");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_06");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest createRequest = new ScheduleRequest();
        createRequest.setCustomerId(customerId);
        createRequest.setEnvironmentId(environmentId);
        createRequest.setMetricId(metricId);
        createRequest.setCronExpression("0 * * * *");
        createRequest.setEnabled(true);

        String createResponse = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn().getResponse().getContentAsString();

        Long scheduleId = objectMapper.readTree(createResponse).get("id").asLong();

        ScheduleRequest updateRequest = new ScheduleRequest();
        updateRequest.setCustomerId(customerId);
        updateRequest.setEnvironmentId(environmentId);
        updateRequest.setMetricId(metricId);
        updateRequest.setCronExpression("0 0 * * *");
        updateRequest.setEnabled(false);

        mockMvc.perform(put("/api/v1/schedules/" + scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cronExpression").value("0 0 0 * * *"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void deleteSchedule_succeeds() throws Exception {
        Long customerId = createCustomer("SCHEDTEST7");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_07");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest request = new ScheduleRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricId(metricId);
        request.setCronExpression("0 * * * *");

        String createResponse = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long scheduleId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/v1/schedules/" + scheduleId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/schedules/" + scheduleId))
                .andExpect(status().isNotFound());
    }

    /**
     * Regression test for a real bug found via manual UI testing: deleting a
     * schedule that had already executed (so a Run row referenced it via
     * schedule_id) threw an unhandled foreign-key violation, which the
     * frontend silently swallowed — the Delete button appeared to do nothing.
     * Fix: ScheduleService.delete() now detaches referencing runs first.
     */
    @Test
    @org.springframework.transaction.annotation.Transactional
    void deleteSchedule_thatHasAlreadyExecuted_stillSucceedsAndPreservesRunHistory() throws Exception {
        Long customerId = createCustomer("SCHEDTEST8");
        Long environmentId = createEnvironment(customerId, "PROD");
        Long metricId = createMetric("SVC_SCHEDULE_TEST_08");
        assignMetricToCustomer(customerId, metricId);

        ScheduleRequest request = new ScheduleRequest();
        request.setCustomerId(customerId);
        request.setEnvironmentId(environmentId);
        request.setMetricId(metricId);
        request.setCronExpression("0 * * * *");

        String createResponse = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        Long scheduleId = objectMapper.readTree(createResponse).get("id").asLong();

        // Simulate the schedule having actually fired at least once, the way
        // SchedulePollingService would, creating a Run row with schedule_id set.
        com.appworks.portal.entity.Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        monitoringService.executeSchedule(schedule);

        long runCountBefore = runRepository.count();

        // This delete must succeed (204), not throw a raw FK-violation 500.
        mockMvc.perform(delete("/api/v1/schedules/" + scheduleId))
                .andExpect(status().isNoContent());

        // The run created by that execution must still exist afterward —
        // deleting a schedule must not silently destroy run history.
        long runCountAfter = runRepository.count();
        org.junit.jupiter.api.Assertions.assertEquals(runCountBefore, runCountAfter);
    }
}
