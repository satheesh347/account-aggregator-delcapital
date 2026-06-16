package com.delcapital.aa.service.impl;

import com.delcapital.aa.dto.request.FetchDataRequest;
import com.delcapital.aa.dto.response.AccountResponse;
import com.delcapital.aa.dto.response.FetchSessionResponse;
import com.delcapital.aa.dto.response.TransactionResponse;
import com.delcapital.aa.entity.*;
import com.delcapital.aa.enums.ConsentStatus;
import com.delcapital.aa.enums.FetchStatus;
import com.delcapital.aa.exception.ConsentNotFoundException;
import com.delcapital.aa.repository.*;
import com.delcapital.aa.service.AuditService;
import com.delcapital.aa.service.DigioApiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FiDataService {

    private final ConsentRequestRepository consentRepo;
    private final FiFetchSessionRepository sessionRepo;
    private final FiAccountRepository accountRepo;
    private final FiTransactionRepository txnRepo;
    private final DigioApiService digioApiService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Initiate a data fetch for an active consent. Idempotent by idempotencyKey.
     */
    @Transactional
    public FetchSessionResponse initiateDataFetch(FetchDataRequest req, String actor, String ip) {
        // Idempotency
        String iKey = req.getIdempotencyKey() != null ? req.getIdempotencyKey() : UUID.randomUUID().toString();
        Optional<FiFetchSession> existing = sessionRepo.findByIdempotencyKey(iKey);
        if (existing.isPresent()) {
            log.info("Returning existing fetch session for idempotencyKey={}", iKey);
            return toSessionResponse(existing.get());
        }

        // Validate consent
        ConsentRequest consent = consentRepo.findById(req.getConsentId())
                .orElseThrow(() -> new ConsentNotFoundException(req.getConsentId()));
        if (consent.getStatus() != ConsentStatus.ACTIVE) {
            throw new IllegalStateException("Consent is not ACTIVE. Current status: " + consent.getStatus());
        }

        List<String> fiTypes = req.getFiTypes() != null && !req.getFiTypes().isEmpty()
                ? req.getFiTypes()
                : List.of(consent.getFiTypes());

        // Save session record
        FiFetchSession session = FiFetchSession.builder()
                .consentRequest(consent)
                .status(FetchStatus.INITIATED)
                .fiTypes(fiTypes.toArray(new String[0]))
                .dateRangeFrom(req.getDateRangeFrom())
                .dateRangeTo(req.getDateRangeTo())
                .idempotencyKey(iKey)
                .rawRequest(safeJson(req))
                .build();
        session = sessionRepo.save(session);

        // Call Digio
        try {
            JsonNode digioResponse = digioApiService.initiateDataFetch(
                    consent.getDigioConsentId(),
                    fiTypes,
                    req.getDateRangeFrom().toString(),
                    req.getDateRangeTo().toString(),
                    consent.getCallbackUrl()
            );

            String digioSessionId = getTextOrNull(digioResponse, "id");
            String status = getTextOrNull(digioResponse, "status");

            session.setDigioSessionId(digioSessionId);
            session.setStatus(FetchStatus.PROCESSING);
            session.setRawResponse(digioResponse.toString());
            session = sessionRepo.save(session);

            auditService.log("FETCH", session.getId(), "INITIATED", actor, ip,
                    "digioSessionId=" + digioSessionId);

            // Kick off async download if immediately available
            if ("COMPLETED".equalsIgnoreCase(status) && digioSessionId != null) {
                downloadAndPersistAsync(session.getId(), digioSessionId);
            }

        } catch (Exception e) {
            log.error("Data fetch initiation failed: {}", e.getMessage());
            session.setStatus(FetchStatus.FAILED);
            session.setErrorMessage(e.getMessage());
            sessionRepo.save(session);
            throw new RuntimeException("Data fetch failed: " + e.getMessage(), e);
        }

        return toSessionResponse(session);
    }

    /**
     * Get fetch session status — polls Digio if still PROCESSING.
     */
    @Transactional
    public FetchSessionResponse getFetchSessionStatus(UUID sessionId) {
        FiFetchSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Fetch session not found: " + sessionId));

        if (session.getStatus() == FetchStatus.PROCESSING && session.getDigioSessionId() != null) {
            try {
                JsonNode digioStatus = digioApiService.getDataFetchStatus(session.getDigioSessionId());
                String status = getTextOrNull(digioStatus, "status");
                if ("COMPLETED".equalsIgnoreCase(status)) {
                    downloadAndPersistAsync(sessionId, session.getDigioSessionId());
                } else if ("FAILED".equalsIgnoreCase(status)) {
                    session.setStatus(FetchStatus.FAILED);
                    session.setErrorCode(getTextOrNull(digioStatus, "error_code"));
                    session.setErrorMessage(getTextOrNull(digioStatus, "error_message"));
                    sessionRepo.save(session);
                }
            } catch (Exception e) {
                log.warn("Failed to poll Digio fetch status: {}", e.getMessage());
            }
        }

        return toSessionResponse(session);
    }

    /**
     * Get normalized accounts for a fetch session.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsForSession(UUID sessionId) {
        return accountRepo.findBySessionId(sessionId).stream()
                .map(this::toAccountResponse)
                .toList();
    }

    /**
     * Handle webhook notification for data fetch completion.
     */
    @Transactional
    public void handleFetchWebhook(String digioSessionId, String status, String rawPayload) {
        Optional<FiFetchSession> sessionOpt = sessionRepo.findByDigioSessionId(digioSessionId);
        if (sessionOpt.isEmpty()) {
            log.warn("Received webhook for unknown digioSessionId={}", digioSessionId);
            return;
        }
        FiFetchSession session = sessionOpt.get();
        if ("COMPLETED".equalsIgnoreCase(status)) {
            session.setStatus(FetchStatus.PROCESSING);
            sessionRepo.save(session);
            downloadAndPersistAsync(session.getId(), digioSessionId);
        } else if ("FAILED".equalsIgnoreCase(status)) {
            session.setStatus(FetchStatus.FAILED);
            session.setErrorMessage("Digio fetch failed per webhook notification");
            sessionRepo.save(session);
        }
    }

    // ─── Async download & normalization ──────────────────────────────────────

    @Async
    @Transactional
    public void downloadAndPersistAsync(UUID sessionId, String digioSessionId) {
        log.info("Downloading FI data for sessionId={} digioSessionId={}", sessionId, digioSessionId);
        try {
            JsonNode data = digioApiService.downloadFiData(digioSessionId);
            FiFetchSession session = sessionRepo.findById(sessionId).orElseThrow();
            normalizeAndPersist(session, data);
            session.setStatus(FetchStatus.COMPLETED);
            session.setFetchedAt(OffsetDateTime.now());
            sessionRepo.save(session);
            auditService.log("FETCH", sessionId, "COMPLETED", "SYSTEM", null, "accounts saved");
        } catch (Exception e) {
            log.error("Failed to download/persist FI data for session {}: {}", sessionId, e.getMessage());
            sessionRepo.findById(sessionId).ifPresent(s -> {
                s.setStatus(FetchStatus.FAILED);
                s.setErrorMessage(e.getMessage());
                sessionRepo.save(s);
            });
        }
    }

    /**
     * Normalize Digio FI payload into canonical models and persist.
     */
    private void normalizeAndPersist(FiFetchSession session, JsonNode data) {
        JsonNode accounts = data.path("accounts");
        if (!accounts.isArray()) {
            log.warn("No accounts array in FI data response");
            return;
        }

        for (JsonNode accountNode : accounts) {
            FiAccount account = FiAccount.builder()
                    .session(session)
                    .fipId(getTextOrNull(accountNode, "fipId"))
                    .accountRef(getTextOrNull(accountNode, "linkRefNumber"))
                    .accountType(getTextOrNull(accountNode, "accountType"))
                    .fiType(getTextOrNull(accountNode, "fiType"))
                    .maskedAccNo(getTextOrNull(accountNode, "maskedAccNumber"))
                    .holderName(getTextOrNull(accountNode, "Profile.Holders.Holder[0].name"))
                    .currency("INR")
                    .rawPayload(accountNode.toString())
                    .build();

            // Extract summary/balance
            JsonNode summary = accountNode.path("Summary");
            if (!summary.isMissingNode()) {
                account.setBalance(parseBigDecimal(summary, "currentBalance"));
                account.setIfscCode(getTextOrNull(summary, "ifscCode"));
                account.setBranch(getTextOrNull(summary, "branch"));
                String dateStr = getTextOrNull(summary, "currentODLimit");
                // asOfDate from pending field
            }
            account = accountRepo.save(account);

            // Normalize transactions
            JsonNode transactions = accountNode.path("Transactions").path("Transaction");
            if (transactions.isArray()) {
                List<FiTransaction> txns = new ArrayList<>();
                for (JsonNode txnNode : transactions) {
                    FiTransaction txn = FiTransaction.builder()
                            .account(account)
                            .txnId(getTextOrNull(txnNode, "txnId"))
                            .txnDate(parseDate(getTextOrNull(txnNode, "valueDate")))
                            .amount(parseBigDecimal(txnNode, "amount"))
                            .txnType(getTextOrNull(txnNode, "type"))
                            .mode(getTextOrNull(txnNode, "mode"))
                            .narration(getTextOrNull(txnNode, "narration"))
                            .reference(getTextOrNull(txnNode, "reference"))
                            .balance(parseBigDecimal(txnNode, "currentBalance"))
                            .build();
                    txns.add(txn);
                }
                txnRepo.saveAll(txns);
                log.debug("Persisted {} transactions for account {}", txns.size(), account.getId());
            }
        }
        log.info("Normalized and persisted FI data for sessionId={}", session.getId());
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private FetchSessionResponse toSessionResponse(FiFetchSession s) {
        return FetchSessionResponse.builder()
                .id(s.getId())
                .consentId(s.getConsentRequest().getId())
                .digioSessionId(s.getDigioSessionId())
                .status(s.getStatus())
                .fiTypes(s.getFiTypes())
                .dateRangeFrom(s.getDateRangeFrom())
                .dateRangeTo(s.getDateRangeTo())
                .errorCode(s.getErrorCode())
                .errorMessage(s.getErrorMessage())
                .fetchedAt(s.getFetchedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private AccountResponse toAccountResponse(FiAccount a) {
        List<TransactionResponse> txns = txnRepo.findByAccountIdAndTxnDateBetween(
                a.getId(), LocalDate.now().minusYears(1), LocalDate.now()
        ).stream().map(t -> TransactionResponse.builder()
                .id(t.getId()).txnId(t.getTxnId()).txnDate(t.getTxnDate())
                .amount(t.getAmount()).txnType(t.getTxnType()).mode(t.getMode())
                .narration(t.getNarration()).reference(t.getReference()).balance(t.getBalance())
                .build()).toList();

        return AccountResponse.builder()
                .id(a.getId()).fipId(a.getFipId()).accountType(a.getAccountType())
                .fiType(a.getFiType()).maskedAccNo(a.getMaskedAccNo()).currency(a.getCurrency())
                .holderName(a.getHolderName()).ifscCode(a.getIfscCode()).branch(a.getBranch())
                .balance(a.getBalance()).asOfDate(a.getAsOfDate()).transactions(txns)
                .build();
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (!n.isMissingNode() && !n.isNull()) ? n.asText() : null;
    }

    private BigDecimal parseBigDecimal(JsonNode node, String field) {
        try {
            String val = getTextOrNull(node, field);
            return val != null ? new BigDecimal(val) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
