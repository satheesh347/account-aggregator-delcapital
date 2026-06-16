package com.delcapital.aa.repository;

import com.delcapital.aa.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    List<WebhookEvent> findByProcessedFalse();
    List<WebhookEvent> findByDigioEntityId(String digioEntityId);
}
