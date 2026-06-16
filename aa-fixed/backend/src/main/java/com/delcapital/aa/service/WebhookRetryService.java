package com.delcapital.aa.service;

import com.delcapital.aa.entity.WebhookEvent;
import com.delcapital.aa.repository.WebhookEventRepository;
import com.delcapital.aa.service.impl.ConsentService;
import com.delcapital.aa.service.impl.FiDataService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Retries webhook events that were received but not processed
 * (e.g. due to transient DB failures).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookRetryService {

    private final WebhookEventRepository webhookEventRepo;
    private final ConsentService consentService;
    private final FiDataService fiDataService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 60_000) // every 60 seconds
    @Transactional
    public void retryUnprocessed() {
        List<WebhookEvent> pending = webhookEventRepo.findByProcessedFalse();
        if (pending.isEmpty()) return;

        log.info("Retrying {} unprocessed webhook events", pending.size());
        for (WebhookEvent event : pending) {
            try {
                JsonNode node = objectMapper.readTree(event.getPayload());
                String eventType = getTextOrNull(node, "event_type");
                String entityId  = getTextOrNull(node, "id");
                String status    = getTextOrNull(node, "status");

                if ("CONSENT_STATUS_UPDATE".equalsIgnoreCase(eventType) && entityId != null) {
                    consentService.updateConsentStatus(entityId, status, event.getPayload());
                } else if ("DATA_FETCH_STATUS_UPDATE".equalsIgnoreCase(eventType) && entityId != null) {
                    fiDataService.handleFetchWebhook(entityId, status, event.getPayload());
                }

                event.setProcessed(true);
                event.setProcessedAt(OffsetDateTime.now());
                event.setError(null);
            } catch (Exception e) {
                log.warn("Retry failed for webhook {}: {}", event.getId(), e.getMessage());
                event.setError(e.getMessage());
            }
            webhookEventRepo.save(event);
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (!n.isMissingNode() && !n.isNull()) ? n.asText() : null;
    }
}
