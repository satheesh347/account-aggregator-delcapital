package com.delcapital.aa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fi_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FiTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FiAccount account;

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "txn_date")
    private LocalDate txnDate;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "txn_type")
    private String txnType;  // CREDIT | DEBIT

    private String mode;  // NEFT, IMPS, UPI, etc.

    private String narration;

    private String reference;

    @Column(precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
