package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JiraIntegrationRepository
        extends JpaRepository<JiraIntegration, Long> {

    Optional<JiraIntegration> findByJiraWebhookId(String jiraWebhookId);
}