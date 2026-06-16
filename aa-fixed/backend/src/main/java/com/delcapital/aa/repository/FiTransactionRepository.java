package com.delcapital.aa.repository;

import com.delcapital.aa.entity.FiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FiTransactionRepository extends JpaRepository<FiTransaction, UUID> {
    Page<FiTransaction> findByAccountId(UUID accountId, Pageable pageable);
    List<FiTransaction> findByAccountIdAndTxnDateBetween(UUID accountId, LocalDate from, LocalDate to);
}
