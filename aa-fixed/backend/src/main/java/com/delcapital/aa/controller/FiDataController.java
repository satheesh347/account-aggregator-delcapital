package com.delcapital.aa.controller;

import com.delcapital.aa.dto.request.FetchDataRequest;
import com.delcapital.aa.dto.response.AccountResponse;
import com.delcapital.aa.dto.response.ApiResponse;
import com.delcapital.aa.dto.response.FetchSessionResponse;
import com.delcapital.aa.service.impl.FiDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/fi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FI Data", description = "Financial Information data fetch and retrieval APIs")
public class FiDataController {

    private final FiDataService fiDataService;

    /**
     * Initiate a data fetch session for an active consent.
     */
    @PostMapping("/fetch")
    @Operation(summary = "Initiate FI data fetch",
               description = "Triggers data retrieval from FIPs for the given consent. Idempotent.")
    public ResponseEntity<ApiResponse<FetchSessionResponse>> initiateDataFetch(
            @Valid @RequestBody FetchDataRequest req,
            HttpServletRequest httpReq) {

        FetchSessionResponse response = fiDataService.initiateDataFetch(
                req,
                httpReq.getHeader("X-User-Id"),
                httpReq.getRemoteAddr()
        );
        return ResponseEntity.ok(ApiResponse.success("Data fetch initiated.", response));
    }

    /**
     * Poll fetch session status.
     */
    @GetMapping("/fetch/{sessionId}")
    @Operation(summary = "Get fetch session status")
    public ResponseEntity<ApiResponse<FetchSessionResponse>> getFetchStatus(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(fiDataService.getFetchSessionStatus(sessionId)));
    }

    /**
     * Get normalized financial accounts for a completed fetch session.
     */
    @GetMapping("/fetch/{sessionId}/accounts")
    @Operation(summary = "Get normalized accounts for fetch session",
               description = "Returns canonical account and transaction data after fetch completes.")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ApiResponse.success(fiDataService.getAccountsForSession(sessionId)));
    }
}
