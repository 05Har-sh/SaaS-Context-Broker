package com.harsh.context_broker.contextBroker.entity;

import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false)
    private JiraIntegrationType integrationType;

    @Column(name = "cloud_id")
    private String cloudId;

    @Column(name = "site_url")
    private String siteUrl;

    @Column(name = "site_name")
    private String siteName;

    @Column(name = "access_token_encrypted")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted")
    private String refreshTokenEncrypted;

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

    public JiraIntegrationType getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(JiraIntegrationType integrationType) {
        this.integrationType = integrationType;
    }

    public String getCloudId() {
        return cloudId;
    }

    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public void setAccessTokenEncrypted(String accessTokenEncrypted) {
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public void setId(Long id) {
        this.id = id;
    }
}