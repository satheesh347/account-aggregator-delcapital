package com.delcapital.aa.repository;

import com.delcapital.aa.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByExternalId(String externalId);
    Optional<Customer> findByMobile(String mobile);
}
