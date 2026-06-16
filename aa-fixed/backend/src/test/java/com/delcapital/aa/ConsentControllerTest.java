package com.delcapital.aa;

import com.delcapital.aa.dto.request.CreateConsentRequest;
import com.delcapital.aa.service.impl.ConsentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = com.delcapital.aa.controller.ConsentController.class)
class ConsentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ConsentService consentService;

    @Test
    @WithMockUser
    void createConsent_validationFails_whenMobileInvalid() throws Exception {
        CreateConsentRequest req = new CreateConsentRequest();
        req.setCustomerExternalId("CUST-001");
        req.setCustomerName("Test User");
        req.setMobile("12345");  // invalid
        req.setPurposeCode("104");
        req.setFiTypes(List.of("DEPOSIT"));
        req.setConsentStart(OffsetDateTime.now());
        req.setConsentExpiry(OffsetDateTime.now().plusYears(1));
        req.setFetchType("ONETIME");

        mockMvc.perform(post("/v1/consents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    void createConsent_validationFails_whenFiTypesEmpty() throws Exception {
        CreateConsentRequest req = new CreateConsentRequest();
        req.setCustomerExternalId("CUST-001");
        req.setCustomerName("Test User");
        req.setMobile("9876543210");
        req.setPurposeCode("104");
        req.setFiTypes(List.of());  // empty
        req.setConsentStart(OffsetDateTime.now());
        req.setConsentExpiry(OffsetDateTime.now().plusYears(1));
        req.setFetchType("ONETIME");

        mockMvc.perform(post("/v1/consents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
