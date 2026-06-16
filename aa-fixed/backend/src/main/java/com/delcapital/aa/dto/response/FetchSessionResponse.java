package com.delcapital.aa.dto.response;

import com.delcapital.aa.enums.FetchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class FetchSessionResponse {
    private UUID id;
    private UUID consentId;
    private String digioSessionId;
    private FetchStatus status;
    private String[] fiTypes;
    private LocalDate dateRangeFrom;
    private LocalDate dateRangeTo;
    private String errorCode;
    private String errorMessage;
    private OffsetDateTime fetchedAt;
    private OffsetDateTime createdAt;
}
