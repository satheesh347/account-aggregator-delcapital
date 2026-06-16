package com.delcapital.aa;

import com.delcapital.aa.dto.request.CreateConsentRequest;
import com.delcapital.aa.dto.response.ConsentResponse;
import com.delcapital.aa.entity.ConsentRequest;
import com.delcapital.aa.entity.Customer;
import com.delcapital.aa.enums.ConsentStatus;
import com.delcapital.aa.repository.ConsentRequestRepository;
import com.delcapital.aa.repository.CustomerRepository;
import com.delcapital.aa.service.AuditService;
import com.delcapital.aa.service.DigioApiService;
import com.delcapital.aa.service.impl.ConsentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock ConsentRequestRepository consentRepo;
    @Mock CustomerRepository customerRepo;
    @Mock DigioApiService digioApiService;
    @Mock AuditService auditService;

    @InjectMocks ConsentService consentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(consentService, "templateId", "TMPL-TEST");
        ReflectionTestUtils.setField(consentService, "objectMapper", objectMapper);
    }

    @Test
    void createConsent_returnsExistingOnDuplicateIdempotencyKey() {
        String iKey = UUID.randomUUID().toString();
        ConsentRequest existing = ConsentRequest.builder()
                .id(UUID.randomUUID())
                .status(ConsentStatus.PENDING)
                .idempotencyKey(iKey)
                .fiTypes(new String[]{"DEPOSIT"})
                .fetchType("ONETIME")
                .build();

        when(consentRepo.findByIdempotencyKey(iKey)).thenReturn(Optional.of(existing));

        CreateConsentRequest req = buildRequest();
        req.setIdempotencyKey(iKey);

        ConsentResponse response = consentService.createConsent(req, "test-actor", "127.0.0.1");

        assertThat(response.getStatus()).isEqualTo(ConsentStatus.PENDING);
        verify(digioApiService, never()).createConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createConsent_callsDigioAndPersists() {
        when(consentRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());

        Customer savedCustomer = Customer.builder()
                .id(UUID.randomUUID()).name("Test User")
                .mobile("9876543210").externalId("CUST-001").build();

        when(customerRepo.findByExternalId("CUST-001")).thenReturn(Optional.of(savedCustomer));

        ConsentRequest savedConsent = ConsentRequest.builder()
                .id(UUID.randomUUID())
                .customer(savedCustomer)
                .status(ConsentStatus.PENDING)
                .fiTypes(new String[]{"DEPOSIT"})
                .fetchType("ONETIME")
                .idempotencyKey("new-key")
                .templateId("TMPL-TEST")
                .build();

        when(consentRepo.save(any())).thenReturn(savedConsent);

        ObjectNode digioResponse = objectMapper.createObjectNode();
        digioResponse.put("id", "DIGIO-CONSENT-123");
        digioResponse.put("redirect_url", "https://digio.in/consent?token=abc");

        when(digioApiService.createConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(digioResponse);

        CreateConsentRequest req = buildRequest();
        ConsentResponse response = consentService.createConsent(req, "actor", "1.2.3.4");

        assertThat(response).isNotNull();
        verify(digioApiService).createConsent(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(consentRepo, atLeast(1)).save(any());
    }

    @Test
    void updateConsentStatus_updatesStatusCorrectly() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder().id(UUID.randomUUID()).name("User")
                .mobile("9999999999").externalId("X").build();
        ConsentRequest consent = ConsentRequest.builder()
                .id(id).customer(customer)
                .digioConsentId("DIGIO-123")
                .status(ConsentStatus.PENDING)
                .fiTypes(new String[]{"DEPOSIT"})
                .fetchType("ONETIME")
                .idempotencyKey("ik-1")
                .templateId("T")
                .build();

        when(consentRepo.findByDigioConsentId("DIGIO-123")).thenReturn(Optional.of(consent));
        when(consentRepo.save(any())).thenReturn(consent);

        consentService.updateConsentStatus("DIGIO-123", "ACTIVE", "{}");

        assertThat(consent.getStatus()).isEqualTo(ConsentStatus.ACTIVE);
        verify(auditService).log(eq("CONSENT"), eq(id), eq("STATUS_CHANGE"), any(), any(), any(), any());
    }

    private CreateConsentRequest buildRequest() {
        CreateConsentRequest req = new CreateConsentRequest();
        req.setCustomerExternalId("CUST-001");
        req.setCustomerName("Test User");
        req.setMobile("9876543210");
        req.setPurposeCode("104");
        req.setFiTypes(List.of("DEPOSIT"));
        req.setConsentStart(OffsetDateTime.now());
        req.setConsentExpiry(OffsetDateTime.now().plusYears(1));
        req.setFetchType("ONETIME");
        return req;
    }
}
