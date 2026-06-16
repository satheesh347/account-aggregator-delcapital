package com.delcapital.aa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Digio Account Aggregator API client.
 * All external calls go through this class — centralized retry/circuit-breaker
 * logic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DigioApiService {

    @Qualifier("digioWebClient")
    private final WebClient digioWebClient;
    private final ObjectMapper objectMapper;

    @Value("${digio.template-id}")
    private String templateId;

    // ─── Consent ─────────────────────────────────────────────────────────────

    /**
     * Create a consent request in Digio using the configured template.
     * POST /v2/client/consent/request/create/{templateId}
     */
    @CircuitBreaker(name = "digioClient", fallbackMethod = "createConsentFallback")
    @Retry(name = "digioClient")
    public JsonNode createConsent(String customerName, String mobile, String email,
            OffsetDateTime consentStart, OffsetDateTime consentExpiry,
            List<String> fiTypes, String purposeCode, String purposeText,
            String fetchType, String notifyUrl) {
        log.info("Creating Digio consent for mobile={}", mobile);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("customer_identifier", mobile);
        payload.put("customer_name", customerName);
        if (email != null)
            payload.put("customer_email", email);
        payload.put("notify_customer", true);

        // AA Consent fields
        ObjectNode consent = objectMapper.createObjectNode();
        consent.put("start_datetime", consentStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        consent.put("expiry_datetime", consentExpiry.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        consent.put("consent_type", "STORE");
        consent.put("fetch_type", fetchType);
        consent.set("fi_types", objectMapper.valueToTree(fiTypes));

        ObjectNode purpose = objectMapper.createObjectNode();
        purpose.put("code", purposeCode);
        purpose.put("text", purposeText != null ? purposeText : "Financial data access for credit decisioning");
        consent.set("purpose", purpose);

        payload.set("consent", consent);

        if (notifyUrl != null)
            payload.put("notify_url", notifyUrl);
        log.info("DIGIO REQUEST = {}", payload.toPrettyString());

        return digioWebClient.post()
                .uri("/v2/client/consent/request/create/{templateId}", templateId)
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class).flatMap(body -> {
                    log.error("Digio createConsent error: status={} body={}", response.statusCode(), body);
                    return Mono.error(new RuntimeException("Digio API error: " + body));
                }))
                .bodyToMono(JsonNode.class)
                .block();
    }

    /**
     * Get consent status from Digio.
     * GET /v2/client/consent/{consentId}
     */
    @CircuitBreaker(name = "digioClient")
    @Retry(name = "digioClient")
    public JsonNode getConsentStatus(String digioConsentId) {
        log.debug("Fetching Digio consent status for consentId={}", digioConsentId);
        return digioWebClient.get()
                .uri("/v2/client/consent/{consentId}", digioConsentId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    // ─── Data Fetch ──────────────────────────────────────────────────────────

    /**
     * Initiate a data fetch session in Digio.
     * POST /v2/client/data/fetch/request
     */
    @CircuitBreaker(name = "digioClient")
    @Retry(name = "digioClient")
    public JsonNode initiateDataFetch(String digioConsentId, List<String> fiTypes,
            String dateFrom, String dateTo, String notifyUrl) {
        log.info("Initiating data fetch for consentId={}", digioConsentId);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("consent_id", digioConsentId);
        payload.set("fi_types", objectMapper.valueToTree(fiTypes));

        ObjectNode dateRange = objectMapper.createObjectNode();
        dateRange.put("from", dateFrom);
        dateRange.put("to", dateTo);
        payload.set("date_range", dateRange);

        if (notifyUrl != null)
            payload.put("notify_url", notifyUrl);

        return digioWebClient.post()
                .uri("/v2/client/data/fetch/request")
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class).flatMap(body -> {
                    log.error("Digio initiateDataFetch error: {}", body);
                    return Mono.error(new RuntimeException("Digio data fetch error: " + body));
                }))
                .bodyToMono(JsonNode.class)
                .block();
    }

    /**
     * Poll data fetch session status.
     * GET /v2/client/data/fetch/{sessionId}
     */
    @CircuitBreaker(name = "digioClient")
    @Retry(name = "digioClient")
    public JsonNode getDataFetchStatus(String digioSessionId) {
        log.debug("Polling data fetch status for sessionId={}", digioSessionId);
        return digioWebClient.get()
                .uri("/v2/client/data/fetch/{sessionId}", digioSessionId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    /**
     * Download the actual FI data.
     * GET /v2/client/data/fetch/{sessionId}/accounts
     */
    @CircuitBreaker(name = "digioClient")
    public JsonNode downloadFiData(String digioSessionId) {
        log.info("Downloading FI data for sessionId={}", digioSessionId);
        return digioWebClient.get()
                .uri("/v2/client/data/fetch/{sessionId}/accounts", digioSessionId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    // ─── Fallbacks ───────────────────────────────────────────────────────────

    public JsonNode createConsentFallback(String customerName, String mobile, String email,
            OffsetDateTime consentStart, OffsetDateTime consentExpiry,
            List<String> fiTypes, String purposeCode, String purposeText,
            String fetchType, String notifyUrl, Throwable t) {
        log.error("Digio createConsent circuit OPEN or max retries exceeded: {}", t.getMessage());
        throw new RuntimeException("Digio service unavailable. Please retry after some time.", t);
    }
}
