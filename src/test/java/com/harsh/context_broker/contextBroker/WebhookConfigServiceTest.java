package com.harsh.context_broker.contextBroker;

import com.harsh.context_broker.contextBroker.entity.WebhookConfig;
import com.harsh.context_broker.contextBroker.repository.WebhookConfigRepository;
import com.harsh.context_broker.contextBroker.service.WebhookConfigService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tenant context is required before Slack reads/writes
 * Tenant context is required before Jira writes/reads
 * Slack configuration is stored separately per tenant
 * Jira configuration is stored separately per tenant
 * Tenant A cannot leak into Tenant B
 * Slack and Jira values remain independent within the same tenant
 * Unconfigured webhook returns null
 */
@SpringBootTest
class WebhookConfigServiceTest {

    @Autowired
    private WebhookConfigRepository repository;

    @Autowired
    private WebhookConfigService service;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        repository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        repository.deleteAll();
    }

    @Test
    void shouldRequireTenantContextBeforeSavingSlackWebhook() {
        assertThatThrownBy(() -> service.saveWebhook("https://hooks.slack.com/services/T123/B123/ABC"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context available");
    }

    @Test
    void shouldRequireTenantContextBeforeSavingJiraWebhook() {
        assertThatThrownBy(() -> service.saveJiraWebhookUrl("https://jira.example.com/webhook"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context available");
    }

    @Test
    void shouldRequireTenantContextBeforeReadingSlackWebhook() {
        assertThatThrownBy(() -> service.getWebhook())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context available");
    }

    @Test
    void shouldRequireTenantContextBeforeReadingJiraWebhook() {
        assertThatThrownBy(() -> service.getJiraWebhook())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context available");
    }

    @Test
    void shouldStoreSlackWebhookSeparatelyPerTenant() {
        TenantContext.setTenantId("tenant-A");
        service.saveWebhook("https://hooks.slack.com/services/TENANTA/ABC123");

        TenantContext.setTenantId("tenant-B");
        service.saveWebhook("https://hooks.slack.com/services/TENANTB/XYZ456");

        TenantContext.setTenantId("tenant-A");
        assertThat(service.getWebhook())
                .isEqualTo("https://hooks.slack.com/services/TENANTA/ABC123");

        TenantContext.setTenantId("tenant-B");
        assertThat(service.getWebhook())
                .isEqualTo("https://hooks.slack.com/services/TENANTB/XYZ456");

        assertThat(repository.findAll())
                .extracting(WebhookConfig::getTenantId)
                .containsExactlyInAnyOrder("tenant-A", "tenant-B");
    }

    @Test
    void shouldStoreJiraWebhookSeparatelyPerTenant() {
        TenantContext.setTenantId("tenant-A");
        service.saveJiraWebhookUrl("https://jira.example.com/tenant-a-hook");

        TenantContext.setTenantId("tenant-B");
        service.saveJiraWebhookUrl("https://jira.example.com/tenant-b-hook");

        TenantContext.setTenantId("tenant-A");
        assertThat(service.getJiraWebhook()).isEqualTo("https://jira.example.com/tenant-a-hook");

        TenantContext.setTenantId("tenant-B");
        assertThat(service.getJiraWebhook()).isEqualTo("https://jira.example.com/tenant-b-hook");

        assertThat(repository.findAll())
                .extracting(WebhookConfig::getTenantId)
                .containsExactlyInAnyOrder("tenant-A", "tenant-B");
    }

    @Test
    void shouldKeepSlackAndJiraWebhookValuesIndependentWithinSameTenant() {
        TenantContext.setTenantId("tenant-A");
        service.saveWebhook("https://hooks.slack.com/services/SLACKA/ONE");
        service.saveJiraWebhookUrl("https://jira.example.com/jira-a");

        assertThat(service.getWebhook()).isEqualTo("https://hooks.slack.com/services/SLACKA/ONE");
        assertThat(service.getJiraWebhook()).isEqualTo("https://jira.example.com/jira-a");

        assertThat(repository.findAll())
                .singleElement()
                .satisfies(config -> {
                    assertThat(config.getTenantId()).isEqualTo("tenant-A");
                    assertThat(config.getWebhookUrl()).isEqualTo("https://hooks.slack.com/services/SLACKA/ONE");
                    assertThat(config.getJiraWebhookUrl()).isEqualTo("https://jira.example.com/jira-a");
                });
    }

    @Test
    void shouldReturnNullForUnsetWebhookForTenant() {
        TenantContext.setTenantId("tenant-A");
        assertThat(service.getWebhook()).isNull();
        assertThat(service.getJiraWebhook()).isNull();
    }

    @Test
    void shouldNotLeakOtherTenantWebhookConfigIntoCurrentTenant() {
        repository.saveAndFlush(config("tenant-A", "https://hooks.slack.com/services/AA/ONE", "https://jira.example.com/tenant-a"));
        repository.saveAndFlush(config("tenant-B", "https://hooks.slack.com/services/BB/TWO", "https://jira.example.com/tenant-b"));

        TenantContext.setTenantId("tenant-A");
        assertThat(service.getWebhook()).isEqualTo("https://hooks.slack.com/services/AA/ONE");
        assertThat(service.getJiraWebhook()).isEqualTo("https://jira.example.com/tenant-a");

        TenantContext.setTenantId("tenant-B");
        assertThat(service.getWebhook()).isEqualTo("https://hooks.slack.com/services/BB/TWO");
        assertThat(service.getJiraWebhook()).isEqualTo("https://jira.example.com/tenant-b");
    }

    @Test
    void shouldCreateDistinctConfigRowsWhenDifferentTenantsSaveDifferentValues() {
        TenantContext.setTenantId("tenant-A");
        service.saveWebhook("https://hooks.slack.com/services/TENANT_A/123");
        service.saveJiraWebhookUrl("https://jira.example.com/a");

        TenantContext.setTenantId("tenant-B");
        service.saveWebhook("https://hooks.slack.com/services/TENANT_B/456");
        service.saveJiraWebhookUrl("https://jira.example.com/b");

        List<WebhookConfig> configs = repository.findAll();
        assertThat(configs)
                .hasSize(2)
                .extracting(WebhookConfig::getTenantId)
                .containsExactlyInAnyOrder("tenant-A", "tenant-B");

        assertThat(configs)
                .filteredOn(config -> "tenant-A".equals(config.getTenantId()))
                .singleElement()
                .satisfies(config -> {
                    assertThat(config.getWebhookUrl()).isEqualTo("https://hooks.slack.com/services/TENANT_A/123");
                    assertThat(config.getJiraWebhookUrl()).isEqualTo("https://jira.example.com/a");
                });

        assertThat(configs)
                .filteredOn(config -> "tenant-B".equals(config.getTenantId()))
                .singleElement()
                .satisfies(config -> {
                    assertThat(config.getWebhookUrl()).isEqualTo("https://hooks.slack.com/services/TENANT_B/456");
                    assertThat(config.getJiraWebhookUrl()).isEqualTo("https://jira.example.com/b");
                });
    }

    private WebhookConfig config(String tenantId, String slackUrl, String jiraUrl) {
        WebhookConfig config = new WebhookConfig();
        config.setTenantId(tenantId);
        config.setWebhookUrl(slackUrl);
        config.setJiraWebhookUrl(jiraUrl);
        return config;
    }
}
