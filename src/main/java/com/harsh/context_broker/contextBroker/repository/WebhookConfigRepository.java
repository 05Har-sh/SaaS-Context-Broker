package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, Long> {
}
