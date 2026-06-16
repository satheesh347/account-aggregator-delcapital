package com.delcapital.aa.repository;

import com.delcapital.aa.entity.FiAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FiAccountRepository extends JpaRepository<FiAccount, UUID> {
    List<FiAccount> findBySessionId(UUID sessionId);
}
