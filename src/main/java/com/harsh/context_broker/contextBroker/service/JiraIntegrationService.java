package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.client.AtlassianOAuthClient;
import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.model.JiraConnectionStatus;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
import com.harsh.context_broker.contextBroker.model.JiraWebhookHealth;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JiraIntegrationService {
    private final JiraIntegrationRepository repository;
    private final CredentialEncryptionService encryptionService;
    private final AtlassianOAuthClient atlassianOAuthClient;

    public JiraIntegrationService(JiraIntegrationRepository repository, CredentialEncryptionService encryptionService) {
        this(repository, encryptionService, null);
    }

    @Autowired
    public JiraIntegrationService(
            JiraIntegrationRepository repository,
            CredentialEncryptionService encryptionService,
            AtlassianOAuthClient atlassianOAuthClient
    ) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.atlassianOAuthClient = atlassianOAuthClient;
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
        integration.setConnectionStatus(JiraConnectionStatus.CONNECTED);
        integration.setWebhookHealth(JiraWebhookHealth.HEALTHY);

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

        JiraIntegration existingByWebhook = repository.findByJiraWebhookId(jiraWebhookId).orElse(null);
        if (existingByWebhook != null && !existingByWebhook.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Jira site already connected to another tenant");
        }
        JiraIntegration existingByCloud = repository.findByTenantIdAndCloudId(tenantId, cloudId).orElse(null);
        if (existingByCloud != null && !existingByCloud.getId().equals(existingByWebhook != null ? existingByWebhook.getId() : null)) {
            throw new IllegalStateException("Jira cloud site is already configured for this tenant");
        }

        JiraIntegration integration = existingByWebhook != null ? existingByWebhook : existingByCloud != null ? existingByCloud : new JiraIntegration();
        integration.setTenantId(tenantId);
        integration.setJiraWebhookId(jiraWebhookId);
        integration.setIntegrationType(JiraIntegrationType.OAUTH);
        integration.setCloudId(cloudId);
        integration.setSiteUrl(siteUrl);
        integration.setSiteName(siteName);
        integration.setAccessTokenEncrypted(encryptionService.encrypt(accessToken));
        if (refreshToken != null && !refreshToken.isBlank()) {
            integration.setRefreshTokenEncrypted(encryptionService.encrypt(refreshToken));
        }
        integration.setConnectionStatus(JiraConnectionStatus.CONNECTED);
        integration.setWebhookHealth(JiraWebhookHealth.HEALTHY);

        return repository.save(integration);
    }

    public List<JiraIntegration> getCurrentTenantIntegrations() {
        String tenantId = TenantContext.requireTenantId();
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public JiraIntegration getByIdAndTenant(Long integrationId) {
        String tenantId = TenantContext.requireTenantId();
        return repository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Jira integration not found for this tenant"));
    }

    public JiraIntegration disconnect(Long integrationId) {
        String tenantId = TenantContext.requireTenantId();
        JiraIntegration integration = repository.findByIdAndTenantId(integrationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Jira integration not found for this tenant"));

        boolean remoteCleanupFailed = false;
        if (atlassianOAuthClient != null && integration.getCloudId() != null && integration.getJiraWebhookId() != null) {
            String accessToken = null;
            if (integration.getAccessTokenEncrypted() != null && !integration.getAccessTokenEncrypted().isBlank()) {
                try {
                    accessToken = encryptionService.decrypt(integration.getAccessTokenEncrypted());
                } catch (Exception ignored) {
                    accessToken = null;
                }
            }
            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    atlassianOAuthClient.deleteWebhook(integration.getCloudId(), integration.getJiraWebhookId(), accessToken);
                } catch (Exception e) {
                    remoteCleanupFailed = true;
                }
            }
        }

        integration.setConnectionStatus(JiraConnectionStatus.DISCONNECTED);
        integration.setWebhookHealth(remoteCleanupFailed ? JiraWebhookHealth.FAILED : JiraWebhookHealth.DISABLED);
        integration.setAccessTokenEncrypted(null);
        integration.setRefreshTokenEncrypted(null);
        integration.setWebhookSecret(null);
        return repository.save(integration);
    }
}
