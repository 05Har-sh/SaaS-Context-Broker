package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {

    Optional<WebhookConfig> findByTenantId(String tenantId);
}
