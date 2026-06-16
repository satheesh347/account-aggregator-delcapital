package com.delcapital.aa.entity;

import com.delcapital.aa.enums.FetchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fi_fetch_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiFetchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consent_request_id", nullable = false)
    private ConsentRequest consentRequest;

    @Column(name = "digio_session_id", unique = true)
    private String digioSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FetchStatus status = FetchStatus.INITIATED;

    @Column(name = "fi_types", columnDefinition = "text[]")
    private String[] fiTypes;

    @Column(name = "date_range_from")
    private LocalDate dateRangeFrom;

    @Column(name = "date_range_to")
    private LocalDate dateRangeTo;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "raw_request", columnDefinition = "jsonb")
    private String rawRequest;

    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "fetched_at")
    private OffsetDateTime fetchedAt;

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
