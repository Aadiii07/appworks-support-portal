package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.appworks.portal.dto.CustomerRequest;
import com.appworks.portal.dto.EnvironmentRequest;
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
class EnvironmentControllerTest {

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

    @Test
    void createEnvironment_thenFetchIt_succeeds() throws Exception {
        Long customerId = createCustomer("ENVTEST1");

        EnvironmentRequest envRequest = new EnvironmentRequest();
        envRequest.setName("PROD");
        envRequest.setBaseUrl("https://aw.envtest1.com/api");

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PROD"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.customerId").value(customerId));

        mockMvc.perform(get("/api/v1/customers/" + customerId + "/environments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createEnvironment_forNonexistentCustomer_returns404() throws Exception {
        EnvironmentRequest envRequest = new EnvironmentRequest();
        envRequest.setName("PROD");

        mockMvc.perform(post("/api/v1/customers/999999/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEnvironment_withDuplicateNameForSameCustomer_returns409() throws Exception {
        Long customerId = createCustomer("ENVTEST2");

        EnvironmentRequest envRequest = new EnvironmentRequest();
        envRequest.setName("STAGING");

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void getEnvironment_belongingToDifferentCustomer_returns404() throws Exception {
        Long customerA = createCustomer("ENVTEST3A");
        Long customerB = createCustomer("ENVTEST3B");

        EnvironmentRequest envRequest = new EnvironmentRequest();
        envRequest.setName("DEV");

        String response = mockMvc.perform(post("/api/v1/customers/" + customerA + "/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envRequest)))
                .andReturn().getResponse().getContentAsString();

        Long envId = objectMapper.readTree(response).get("id").asLong();

        // Requesting customer A's environment through customer B's path must 404,
        // not silently return another customer's data.
        mockMvc.perform(get("/api/v1/customers/" + customerB + "/environments/" + envId))
                .andExpect(status().isNotFound());
    }
}
