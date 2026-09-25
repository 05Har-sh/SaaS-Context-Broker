package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.JiraWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JiraWebhookRepository extends JpaRepository<JiraWebhookEvent, Long> {
    boolean existsByTenantIdAndWebhookIdentifier(String tenantId, String webhookIdentifier);
}
