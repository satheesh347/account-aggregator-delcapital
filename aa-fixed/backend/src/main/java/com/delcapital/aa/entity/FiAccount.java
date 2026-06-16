package com.delcapital.aa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fi_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private FiFetchSession session;

    @Column(name = "fip_id")
    private String fipId;

    @Column(name = "account_ref")
    private String accountRef;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "fi_type")
    private String fiType;

    @Column(name = "maskedAccNo")
    private String maskedAccNo;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Column(name = "holder_name")
    private String holderName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    private String branch;

    @Column(precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(name = "as_of_date")
    private LocalDate asOfDate;

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
