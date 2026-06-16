package com.delcapital.aa.entity;

import com.delcapital.aa.enums.ConsentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "consent_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "digio_consent_id", unique = true)
    private String digioConsentId;

    @Column(name = "digio_doc_id")
    private String digioDocId;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentStatus status = ConsentStatus.PENDING;

    @Column(name = "purpose_code", nullable = false)
    private String purposeCode;

    @Column(name = "purpose_text")
    private String purposeText;

    @Column(name = "fetch_type", nullable = false)
    private String fetchType;

    @Column(name = "frequency_unit")
    private String frequencyUnit;

    @Column(name = "frequency_value")
    private Integer frequencyValue;

    @Column(name = "consent_start")
    private OffsetDateTime consentStart;

    @Column(name = "consent_expiry")
    private OffsetDateTime consentExpiry;

    @Column(name = "fi_types", columnDefinition = "text[]")
    private String[] fiTypes;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "callback_url")
    private String callbackUrl;

    @Column(name = "raw_request", columnDefinition = "jsonb")
    private String rawRequest;

    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
