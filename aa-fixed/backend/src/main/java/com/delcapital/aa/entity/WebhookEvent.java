package com.delcapital.aa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "digio_entity_id")
    private String digioEntityId;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private Boolean processed = false;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    private String error;

    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = OffsetDateTime.now();
    }
}
