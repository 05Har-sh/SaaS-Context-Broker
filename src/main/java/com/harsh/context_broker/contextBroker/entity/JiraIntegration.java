package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "jira_integration")
public class JiraIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "jira_webhook_id", nullable = false, unique = true)
    private String jiraWebhookId;

    @Column(name = "webhook_secret", nullable = false)
    private String webhookSecret;

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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