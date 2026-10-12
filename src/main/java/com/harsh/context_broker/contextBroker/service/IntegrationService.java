package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.integration.IntegrationSummary;
import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IntegrationService {
    private final SlackIntegrationRepository slackRepository;
    private final JiraIntegrationRepository jiraRepository;

    public IntegrationService(SlackIntegrationRepository slackRepository, JiraIntegrationRepository jiraRepository) {
        this.slackRepository = slackRepository;
        this.jiraRepository = jiraRepository;
    }

    public List<IntegrationSummary> getAllIntegrations() {
        String tenantId = TenantContext.requireTenantId();

        List<IntegrationSummary> results = new ArrayList<>();

        List<SlackIntegration> slackIntegrations = slackRepository.findAllByTenantId(tenantId);
        for (SlackIntegration s : slackIntegrations) {
            boolean connected = s.getAccessTokenEncrypted() != null && !s.getAccessTokenEncrypted().isBlank();
            String displayName = s.getTeamName() != null && !s.getTeamName().isBlank() ? s.getTeamName() : s.getTeamId();
            results.add(new IntegrationSummary(s.getId(), "SLACK", connected, displayName));
        }

        List<JiraIntegration> jiraIntegrations = jiraRepository.findAllByTenantId(tenantId);
        for (JiraIntegration j : jiraIntegrations) {
            boolean connected;
            if (j.getIntegrationType() != null && j.getIntegrationType().name().equals("OAUTH")) {
                connected = j.getAccessTokenEncrypted() != null && !j.getAccessTokenEncrypted().isBlank();
            } else {
                connected = j.getWebhookSecret() != null && !j.getWebhookSecret().isBlank();
            }
            String displayName = j.getSiteName() != null && !j.getSiteName().isBlank() ? j.getSiteName() : j.getSiteUrl();
            results.add(new IntegrationSummary(j.getId(), "JIRA", connected, displayName));
        }

        return results;
    }
}