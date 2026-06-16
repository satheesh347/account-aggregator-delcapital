package com.delcapital.aa.repository;

import com.delcapital.aa.entity.ConsentRequest;
import com.delcapital.aa.enums.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRequestRepository extends JpaRepository<ConsentRequest, UUID> {

    Optional<ConsentRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<ConsentRequest> findByDigioConsentId(String digioConsentId);

    List<ConsentRequest> findByCustomerIdAndStatus(UUID customerId, ConsentStatus status);

    List<ConsentRequest> findByCustomerId(UUID customerId);

    @Query("SELECT c FROM ConsentRequest c WHERE c.customer.id = :customerId ORDER BY c.createdAt DESC")
    List<ConsentRequest> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
