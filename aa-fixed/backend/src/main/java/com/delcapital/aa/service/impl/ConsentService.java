package com.delcapital.aa.service.impl;

import com.delcapital.aa.dto.request.CreateConsentRequest;
import com.delcapital.aa.dto.response.ConsentResponse;
import com.delcapital.aa.entity.ConsentRequest;
import com.delcapital.aa.entity.Customer;
import com.delcapital.aa.enums.ConsentStatus;
import com.delcapital.aa.exception.ConsentAlreadyExistsException;
import com.delcapital.aa.exception.ConsentNotFoundException;
import com.delcapital.aa.repository.ConsentRequestRepository;
import com.delcapital.aa.repository.CustomerRepository;
import com.delcapital.aa.service.AuditService;
import com.delcapital.aa.service.DigioApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRequestRepository consentRepo;
    private final CustomerRepository customerRepo;
    private final DigioApiService digioApiService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${digio.template-id}")
    private String templateId;

    /**
     * Create a new consent request. Idempotent — returns existing if key matches.
     */
    @Transactional
    public ConsentResponse createConsent(CreateConsentRequest req, String actor, String ipAddress) {
        // Idempotency check
        String idempotencyKey = req.getIdempotencyKey() != null
                ? req.getIdempotencyKey()
                : UUID.randomUUID().toString();

        Optional<ConsentRequest> existing = consentRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Returning existing consent for idempotencyKey={}", idempotencyKey);
            return toResponse(existing.get(), null);
        }

        // Resolve or create customer
        Customer customer = customerRepo.findByExternalId(req.getCustomerExternalId())
                .orElseGet(() -> {
                    Customer c = Customer.builder()
                            .externalId(req.getCustomerExternalId())
                            .name(req.getCustomerName())
                            .mobile(req.getMobile())
                            .email(req.getEmail())
                            .build();
                    return customerRepo.save(c);
                });

        // Persist consent request (PENDING)
        ConsentRequest consentRequest = ConsentRequest.builder()
                .customer(customer)
                .templateId(templateId)
                .status(ConsentStatus.PENDING)
                .purposeCode(req.getPurposeCode())
                .purposeText(req.getPurposeText())
                .fetchType(req.getFetchType())
                .frequencyUnit(req.getFrequencyUnit())
                .frequencyValue(req.getFrequencyValue())
                .consentStart(req.getConsentStart())
                .consentExpiry(req.getConsentExpiry())
                .fiTypes(req.getFiTypes().toArray(new String[0]))
                .idempotencyKey(idempotencyKey)
                .redirectUrl(req.getRedirectUrl())
                .callbackUrl(req.getCallbackUrl())
                .rawRequest(safeJson(req))
                .build();
        consentRequest = consentRepo.save(consentRequest);

        // Call Digio API
        String consentUrl = null;
        try {
            JsonNode digioResponse = digioApiService.createConsent(
                    customer.getName(),
                    customer.getMobile(),
                    customer.getEmail(),
                    req.getConsentStart(),
                    req.getConsentExpiry(),
                    req.getFiTypes(),
                    req.getPurposeCode(),
                    req.getPurposeText(),
                    req.getFetchType(),
                    req.getCallbackUrl()
            );

            log.debug("Digio createConsent response: {}", digioResponse);

            // Extract Digio consent identifiers
            String digioConsentId = getTextOrNull(digioResponse, "id");
            String digioDocId = getTextOrNull(digioResponse, "digio_doc_id");
            consentUrl = getTextOrNull(digioResponse, "redirect_url");
            if (consentUrl == null) consentUrl = getTextOrNull(digioResponse, "access_token");

            consentRequest.setDigioConsentId(digioConsentId);
            consentRequest.setDigioDocId(digioDocId);
            consentRequest.setRawResponse(digioResponse.toString());
            consentRequest = consentRepo.save(consentRequest);

            auditService.log("CONSENT", consentRequest.getId(), "CREATED", actor, ipAddress,
                    "status=PENDING digioConsentId=" + digioConsentId);

        } catch (Exception e) {
            log.error("Digio consent creation failed for idempotencyKey={}: {}", idempotencyKey, e.getMessage());
            consentRequest.setRawResponse("{\"error\": \"" + e.getMessage() + "\"}");
            consentRepo.save(consentRequest);
            throw new RuntimeException("Failed to create consent with Digio: " + e.getMessage(), e);
        }

        return toResponse(consentRequest, consentUrl);
    }

    /**
     * Get consent by internal ID.
     */
    @Transactional(readOnly = true)
    public ConsentResponse getConsent(UUID consentId) {
        ConsentRequest consent = consentRepo.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));

        // Optionally refresh from Digio if PENDING
        if (consent.getStatus() == ConsentStatus.PENDING && consent.getDigioConsentId() != null) {
            try {
                refreshConsentStatus(consent);
            } catch (Exception e) {
                log.warn("Failed to refresh consent status from Digio: {}", e.getMessage());
            }
        }

        return toResponse(consent, null);
    }

    /**
     * List consents for a customer.
     */
    @Transactional(readOnly = true)
    public List<ConsentResponse> getConsentsForCustomer(String externalId) {
        Customer customer = customerRepo.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + externalId));
        return consentRepo.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(c -> toResponse(c, null)).toList();
    }

    /**
     * Update consent status (called by webhook handler).
     */
    @Transactional
    public void updateConsentStatus(String digioConsentId, String newStatus, String rawPayload) {
        ConsentRequest consent = consentRepo.findByDigioConsentId(digioConsentId)
                .orElseThrow(() -> new ConsentNotFoundException("digio:" + digioConsentId));

        ConsentStatus oldStatus = consent.getStatus();
        ConsentStatus resolved = resolveStatus(newStatus);
        consent.setStatus(resolved);
        consent.setRawResponse(rawPayload);
        consentRepo.save(consent);

        auditService.log("CONSENT", consent.getId(), "STATUS_CHANGE", "SYSTEM", null,
                "status=" + oldStatus, "status=" + resolved);
        log.info("Consent {} status changed {} -> {}", digioConsentId, oldStatus, resolved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void refreshConsentStatus(ConsentRequest consent) {
        JsonNode status = digioApiService.getConsentStatus(consent.getDigioConsentId());
        String digioStatus = getTextOrNull(status, "status");
        if (digioStatus != null) {
            consent.setStatus(resolveStatus(digioStatus));
            consentRepo.save(consent);
        }
    }

    private ConsentStatus resolveStatus(String raw) {
        if (raw == null) return ConsentStatus.PENDING;
        return switch (raw.toUpperCase()) {
            case "ACTIVE", "ACCEPTED" -> ConsentStatus.ACTIVE;
            case "PAUSED" -> ConsentStatus.PAUSED;
            case "REVOKED" -> ConsentStatus.REVOKED;
            case "EXPIRED" -> ConsentStatus.EXPIRED;
            case "REJECTED", "DECLINED" -> ConsentStatus.REJECTED;
            default -> ConsentStatus.PENDING;
        };
    }

    private ConsentResponse toResponse(ConsentRequest c, String consentUrl) {
        return ConsentResponse.builder()
                .id(c.getId())
                .digioConsentId(c.getDigioConsentId())
                .digioDocId(c.getDigioDocId())
                .status(c.getStatus())
                .purposeCode(c.getPurposeCode())
                .purposeText(c.getPurposeText())
                .fetchType(c.getFetchType())
                .consentStart(c.getConsentStart())
                .consentExpiry(c.getConsentExpiry())
                .fiTypes(c.getFiTypes())
                .redirectUrl(c.getRedirectUrl())
                .consentUrl(consentUrl)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
