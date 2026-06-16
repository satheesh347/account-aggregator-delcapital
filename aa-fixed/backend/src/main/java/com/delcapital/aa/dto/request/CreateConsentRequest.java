package com.delcapital.aa.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class CreateConsentRequest {

    @NotBlank(message = "Customer external ID is required")
    private String customerExternalId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String mobile;

    private String email;

    @NotBlank(message = "Purpose code is required")
    private String purposeCode;

    private String purposeText;

    @NotEmpty(message = "At least one FI type required")
    private List<String> fiTypes;

    @NotNull(message = "Consent start date required")
    private OffsetDateTime consentStart;

    @NotNull(message = "Consent expiry date required")
    @Future(message = "Consent expiry must be in the future")
    private OffsetDateTime consentExpiry;

    private String fetchType = "ONETIME";  // ONETIME | PERIODIC

    private String frequencyUnit;    // DAY | MONTH | YEAR

    private Integer frequencyValue;

    private String redirectUrl;

    private String callbackUrl;

    /**
     * Caller-provided idempotency key. If not provided, one is generated.
     */
    private String idempotencyKey;
}
