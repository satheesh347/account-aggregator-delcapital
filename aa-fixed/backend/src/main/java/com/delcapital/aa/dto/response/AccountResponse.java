package com.delcapital.aa.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private String fipId;
    private String accountType;
    private String fiType;
    private String maskedAccNo;
    private String currency;
    private String holderName;
    private String ifscCode;
    private String branch;
    private BigDecimal balance;
    private LocalDate asOfDate;
    private List<TransactionResponse> transactions;
}
