package com.appworks.portal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.appworks.portal.dto.CustomerRequest;
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
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createCustomer_thenFetchIt_succeeds() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("Acme Corp");
        request.setCode("ACME");
        request.setRegion("US-EAST");
        request.setContactEmail("ops@acme.com");

        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Acme Corp"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/customers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACME"));
    }

    @Test
    void createCustomer_withDuplicateCode_returns409() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("NexaCo");
        request.setCode("NEXA");
        request.setContactEmail("ops@nexaco.com");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCustomer_withMissingRequiredField_returns400() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setCode("NOCODE");
        // name and contactEmail intentionally omitted

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCustomer_thatDoesNotExist_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/customers/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCustomer_softDeletesIt_recordStaysButInactive() throws Exception {
        CustomerRequest request = new CustomerRequest();
        request.setName("CloudX");
        request.setCode("CLOUDX");
        request.setContactEmail("ops@cloudx.com");

        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/customers/" + id))
                .andExpect(status().isNoContent());

        // Soft delete: the record must still exist (history/FK integrity), just INACTIVE
        mockMvc.perform(get("/api/v1/customers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }
}
