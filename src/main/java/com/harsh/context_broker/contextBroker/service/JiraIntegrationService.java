package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class JiraIntegrationService {
    private final JiraIntegrationRepository repository;
    private final CredentialEncryptionService encryptionService;

    public JiraIntegrationService(JiraIntegrationRepository repository, CredentialEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }


    public JiraIntegration create(
            String jiraWebhookId,
            String webhookSecret
    ) {
        String tenantId = TenantContext.requireTenantId();

        if (repository.findByJiraWebhookId(jiraWebhookId).isPresent()) {
            throw new IllegalArgumentException("Jira integration already exists");
        }

        JiraIntegration integration = new JiraIntegration();

        integration.setTenantId(tenantId);
        integration.setJiraWebhookId(jiraWebhookId);

        integration.setWebhookSecret(encryptionService.encrypt(webhookSecret));

        return repository.save(integration);
    }
}
