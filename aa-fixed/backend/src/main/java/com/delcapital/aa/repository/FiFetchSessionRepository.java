package com.delcapital.aa.repository;

import com.delcapital.aa.entity.FiFetchSession;
import com.delcapital.aa.enums.FetchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiFetchSessionRepository extends JpaRepository<FiFetchSession, UUID> {
    Optional<FiFetchSession> findByIdempotencyKey(String idempotencyKey);
    Optional<FiFetchSession> findByDigioSessionId(String digioSessionId);
    List<FiFetchSession> findByConsentRequestId(UUID consentRequestId);
    List<FiFetchSession> findByStatus(FetchStatus status);
}
