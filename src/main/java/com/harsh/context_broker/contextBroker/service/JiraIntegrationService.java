package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
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


    public JiraIntegration create(String jiraWebhookId, String webhookSecret) {
        String tenantId = TenantContext.requireTenantId();

        if (repository.findByJiraWebhookId(jiraWebhookId).isPresent()) {
            throw new IllegalArgumentException("Jira integration already exists");
        }

        JiraIntegration integration = new JiraIntegration();

        integration.setTenantId(tenantId);
        integration.setJiraWebhookId(jiraWebhookId);

        integration.setIntegrationType(JiraIntegrationType.MANUAL);
        integration.setWebhookSecret(encryptionService.encrypt(webhookSecret));

        return repository.save(integration);
    }
    public JiraIntegration createFromOAuth(
            String cloudId,
            String siteUrl,
            String siteName,
            String accessToken,
            String refreshToken,
            String jiraWebhookId
    ) {
        String tenantId = TenantContext.requireTenantId();

        if (cloudId == null || cloudId.isBlank()) {
            throw new IllegalArgumentException("Jira cloud ID is missing");
        }

        if (siteUrl == null || siteUrl.isBlank()) {
            throw new IllegalArgumentException("Jira site URL is missing");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Jira access token is missing");
        }

        if (jiraWebhookId == null || jiraWebhookId.isBlank()) {
            throw new IllegalArgumentException("Jira webhook ID is missing");
        }

        if (repository.findByJiraWebhookId(jiraWebhookId).isPresent()) {
            throw new IllegalArgumentException(
                    "Jira integration already exists"
            );
        }

        JiraIntegration integration = new JiraIntegration();

        integration.setTenantId(tenantId);
        integration.setJiraWebhookId(jiraWebhookId);
        integration.setIntegrationType(JiraIntegrationType.OAUTH);

        integration.setCloudId(cloudId);
        integration.setSiteUrl(siteUrl);
        integration.setSiteName(siteName);

        integration.setAccessTokenEncrypted(
                encryptionService.encrypt(accessToken)
        );

        if (refreshToken != null && !refreshToken.isBlank()) {
            integration.setRefreshTokenEncrypted(
                    encryptionService.encrypt(refreshToken)
            );
        }

        return repository.save(integration);
    }
}
