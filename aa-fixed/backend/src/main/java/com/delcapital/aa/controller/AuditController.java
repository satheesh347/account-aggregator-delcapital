package com.delcapital.aa.controller;

import com.delcapital.aa.dto.response.ApiResponse;
import com.delcapital.aa.entity.AuditLog;
import com.delcapital.aa.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Compliance and audit log retrieval")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for an entity",
               description = "Returns all audit events for a given entity (CONSENT, FETCH, ACCOUNT).")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogs(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<AuditLog> logs = auditLogRepository.findByEntityTypeAndEntityId(
                entityType.toUpperCase(), entityId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
