package com.delcapital.aa.controller;

import com.delcapital.aa.dto.request.CreateConsentRequest;
import com.delcapital.aa.dto.response.ApiResponse;
import com.delcapital.aa.dto.response.ConsentResponse;
import com.delcapital.aa.service.impl.ConsentService;
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
@RequestMapping("/v1/consents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consent", description = "Consent lifecycle management APIs")
public class ConsentController {

    private final ConsentService consentService;

    /**
     * Create a new consent request (customer-facing — initiates Digio flow).
     */
    @PostMapping
    @Operation(summary = "Create consent request",
               description = "Creates a consent request via Digio. Returns a consentUrl for the customer to sign.")
    public ResponseEntity<ApiResponse<ConsentResponse>> createConsent(
            @Valid @RequestBody CreateConsentRequest req,
            HttpServletRequest httpReq) {

        String actor = httpReq.getHeader("X-User-Id");
        String ip = httpReq.getRemoteAddr();

        ConsentResponse response = consentService.createConsent(req, actor, ip);
        return ResponseEntity.ok(ApiResponse.success("Consent request created. Redirect user to consentUrl.", response));
    }

    /**
     * Get consent status by internal ID.
     */
    @GetMapping("/{consentId}")
    @Operation(summary = "Get consent by ID")
    public ResponseEntity<ApiResponse<ConsentResponse>> getConsent(@PathVariable UUID consentId) {
        return ResponseEntity.ok(ApiResponse.success(consentService.getConsent(consentId)));
    }

    /**
     * List all consents for a customer (business-facing).
     */
    @GetMapping("/customer/{externalId}")
    @Operation(summary = "List consents for customer")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> getCustomerConsents(
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.success(consentService.getConsentsForCustomer(externalId)));
    }
}
