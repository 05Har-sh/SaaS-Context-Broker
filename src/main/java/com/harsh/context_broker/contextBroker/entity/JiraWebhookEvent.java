package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "jira_webhook_event", uniqueConstraints = @UniqueConstraint(
        name = "uk_jira_webhook_event_tenant_identifier",
        columnNames = {"tenant_id", "webhook_identifier"}
))
public class JiraWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "webhook_identifier", nullable = false)
    private String webhookIdentifier;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;


    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getWebhookIdentifier() {
        return webhookIdentifier;
    }

    public void setWebhookIdentifier(String webhookIdentifier) {
        this.webhookIdentifier = webhookIdentifier;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
