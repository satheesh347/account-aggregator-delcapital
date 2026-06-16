package com.delcapital.aa.controller;

import com.delcapital.aa.dto.response.ApiResponse;
import com.delcapital.aa.entity.Customer;
import com.delcapital.aa.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer management")
public class CustomerController {

    private final CustomerRepository customerRepository;

    @GetMapping("/{externalId}")
    @Operation(summary = "Get customer by external ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomer(
            @PathVariable String externalId) {
        Customer c = customerRepository.findByExternalId(externalId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + externalId));
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "id", c.getId(),
                "externalId", c.getExternalId(),
                "name", c.getName(),
                "mobile", maskMobile(c.getMobile()),
                "createdAt", c.getCreatedAt()
        )));
    }

    @GetMapping
    @Operation(summary = "List all customers (paginated, business-facing)")
    public ResponseEntity<ApiResponse<Object>> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var result = customerRepository.findAll(pageable).map(c -> Map.of(
                "id", c.getId(),
                "externalId", c.getExternalId(),
                "name", c.getName(),
                "mobile", maskMobile(c.getMobile()),
                "createdAt", c.getCreatedAt()
        ));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "XXXXXX" + mobile.substring(mobile.length() - 4);
    }
}
