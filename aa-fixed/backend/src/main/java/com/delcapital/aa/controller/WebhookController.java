package com.delcapital.aa.controller;

import com.delcapital.aa.dto.response.ApiResponse;
import com.delcapital.aa.entity.WebhookEvent;
import com.delcapital.aa.repository.WebhookEventRepository;
import com.delcapital.aa.service.impl.ConsentService;
import com.delcapital.aa.service.impl.FiDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook", description = "Digio async notification receiver")
public class WebhookController {

    private final WebhookEventRepository webhookEventRepo;
    private final ConsentService consentService;
    private final FiDataService fiDataService;
    private final ObjectMapper objectMapper;

    /**
     * Receive async consent or data fetch notifications from Digio.
     * This endpoint is public (Digio calls it); protect at network level (IP allowlist).
     */
    @PostMapping("/digio")
    @Operation(summary = "Digio async notification handler")
    public ResponseEntity<ApiResponse<Void>> handleDigioWebhook(
            @RequestBody String rawPayload) {

        log.info("Received Digio webhook: {}", rawPayload);

        // Persist raw event first (idempotent, never lose a notification)
        WebhookEvent event = WebhookEvent.builder()
                .payload(rawPayload)
                .build();

        try {
            JsonNode node = objectMapper.readTree(rawPayload);

            String eventType = getTextOrNull(node, "event_type");
            String entityId = getTextOrNull(node, "id");
            String status = getTextOrNull(node, "status");

            event.setEventType(eventType);
            event.setDigioEntityId(entityId);
            webhookEventRepo.save(event);

            // Route by event type
            if ("CONSENT_STATUS_UPDATE".equalsIgnoreCase(eventType) && entityId != null) {
                consentService.updateConsentStatus(entityId, status, rawPayload);
                event.setProcessed(true);
            } else if ("DATA_FETCH_STATUS_UPDATE".equalsIgnoreCase(eventType) && entityId != null) {
                fiDataService.handleFetchWebhook(entityId, status, rawPayload);
                event.setProcessed(true);
            } else {
                log.warn("Unrecognized Digio event type: {}", eventType);
                event.setError("Unknown event type: " + eventType);
            }

        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            event.setError(e.getMessage());
        }

        webhookEventRepo.save(event);
        // Always return 200 so Digio doesn't retry on our processing errors
        return ResponseEntity.ok(ApiResponse.success("Received", null));
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (!n.isMissingNode() && !n.isNull()) ? n.asText() : null;
    }
}
