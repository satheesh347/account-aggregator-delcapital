package com.delcapital.aa.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private String txnId;
    private LocalDate txnDate;
    private BigDecimal amount;
    private String txnType;
    private String mode;
    private String narration;
    private String reference;
    private BigDecimal balance;
}
