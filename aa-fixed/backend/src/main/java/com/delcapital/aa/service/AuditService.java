package com.delcapital.aa.service;

import com.delcapital.aa.entity.AuditLog;
import com.delcapital.aa.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, String action,
                    String actor, String ipAddress, Object newValue) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .actor(actor)
                    .ipAddress(ipAddress)
                    .newValue(newValue != null ? newValue.toString() : null)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: entity={} action={}", entityType, action, e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, UUID entityId, String action,
                    String actor, String ipAddress, Object oldValue, Object newValue) {
        try {
            AuditLog entry = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .actor(actor)
                    .ipAddress(ipAddress)
                    .oldValue(oldValue != null ? oldValue.toString() : null)
                    .newValue(newValue != null ? newValue.toString() : null)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log: entity={} action={}", entityType, action, e);
        }
    }
}
