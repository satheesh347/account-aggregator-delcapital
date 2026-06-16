package com.delcapital.aa.dto.response;

import com.delcapital.aa.enums.ConsentStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ConsentResponse {
    private UUID id;
    private String digioConsentId;
    private String digioDocId;
    private ConsentStatus status;
    private String purposeCode;
    private String purposeText;
    private String fetchType;
    private OffsetDateTime consentStart;
    private OffsetDateTime consentExpiry;
    private String[] fiTypes;
    private String redirectUrl;
    private String consentUrl;      // URL to redirect user for consent signing
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
