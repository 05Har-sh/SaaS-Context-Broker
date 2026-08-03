package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class WebhookConfig {
    @Id
    @GeneratedValue
    private Long id;
    private String webhookUrl; // slack webhookUrl
    private String jiraWebhookUrl; // jira webhookUrl

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getJiraWebhookUrl() {
        return jiraWebhookUrl;
    }

    public void setJiraWebhookUrl(String jiraWebhookUrl) {
        this.jiraWebhookUrl = jiraWebhookUrl;
    }
}
