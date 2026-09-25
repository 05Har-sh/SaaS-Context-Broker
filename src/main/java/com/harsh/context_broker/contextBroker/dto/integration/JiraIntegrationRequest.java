package com.harsh.context_broker.contextBroker.dto.integration;

/**
 * {
 *   "jiraWebhookId": "JIRA-TEST-001",
 *   "webhookSecret": "actual-jira-secret"
 * }
 */
public class JiraIntegrationRequest {
    private String jiraWebhookId;
    private String webhookSecret;

    public String getJiraWebhookId() {
        return jiraWebhookId;
    }

    public void setJiraWebhookId(String jiraWebhookId) {
        this.jiraWebhookId = jiraWebhookId;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
