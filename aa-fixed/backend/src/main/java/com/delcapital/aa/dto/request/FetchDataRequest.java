package com.delcapital.aa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class FetchDataRequest {

    @NotNull(message = "Consent ID is required")
    private UUID consentId;

    private List<String> fiTypes;

    @NotNull(message = "Date range from is required")
    private LocalDate dateRangeFrom;

    @NotNull(message = "Date range to is required")
    private LocalDate dateRangeTo;

    /**
     * Caller-supplied idempotency key for safe retries.
     */
    private String idempotencyKey;
}
