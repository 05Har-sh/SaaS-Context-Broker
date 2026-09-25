package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.JiraWebhookEvent;
import com.harsh.context_broker.contextBroker.repository.JiraWebhookRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class JiraWebhookEventService {
    private final JiraWebhookRepository repository;

    public JiraWebhookEventService(JiraWebhookRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean claimWebhook(String tenantId, String webhookIdentifier) {
        if (repository.existsByTenantIdAndWebhookIdentifier(tenantId, webhookIdentifier)) {
            return false;
        }
        try {
            JiraWebhookEvent event = new JiraWebhookEvent();
            event.setTenantId(tenantId);
            event.setWebhookIdentifier(webhookIdentifier);
            event.setReceivedAt(OffsetDateTime.now());
            repository.saveAndFlush(event);

            return true;
        }catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    public void record(String tenantId, String webhookIdentifier) {
        JiraWebhookEvent event = new JiraWebhookEvent();
        event.setTenantId(tenantId);
        event.setWebhookIdentifier(webhookIdentifier);
        event.setReceivedAt(OffsetDateTime.now());

        repository.save(event);
    }
}
