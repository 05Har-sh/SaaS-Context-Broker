package com.harsh.context_broker.contextBroker.entity;

import com.harsh.context_broker.contextBroker.model.JiraConnectionStatus;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
import com.harsh.context_broker.contextBroker.model.JiraWebhookHealth;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "jira_integration",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "cloud_id"}, name = "uq_jira_integration_tenant_cloud")
        }
)
public class JiraIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "jira_webhook_id", nullable = false, unique = true)
    private String jiraWebhookId;

    @Column(name = "webhook_secret")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false)
    private JiraConnectionStatus connectionStatus = JiraConnectionStatus.CONNECTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "webhook_health", nullable = false)
    private JiraWebhookHealth webhookHealth = JiraWebhookHealth.HEALTHY;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() { return tenantId; }

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

    public JiraConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(JiraConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public JiraWebhookHealth getWebhookHealth() {
        return webhookHealth;
    }

    public void setWebhookHealth(JiraWebhookHealth webhookHealth) {
        this.webhookHealth = webhookHealth;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }
}