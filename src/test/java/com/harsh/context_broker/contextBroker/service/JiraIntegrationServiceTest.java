package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JiraIntegrationServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateManualJiraIntegration() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);

        TenantContext.setTenantId("tenant-123");
        when(repository.findByJiraWebhookId("webhook-1")).thenReturn(Optional.empty());
        when(encryptionService.encrypt("secret-abc")).thenReturn("encrypted-secret");
        when(repository.save(any(JiraIntegration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JiraIntegration integration = service.create("webhook-1", "secret-abc");

        assertThat(integration.getTenantId()).isEqualTo("tenant-123");
        assertThat(integration.getJiraWebhookId()).isEqualTo("webhook-1");
        assertThat(integration.getWebhookSecret()).isEqualTo("encrypted-secret");
        assertThat(integration.getIntegrationType()).isEqualTo(JiraIntegrationType.MANUAL);
        verify(encryptionService).encrypt("secret-abc");
        verify(repository).save(any(JiraIntegration.class));
    }

    @Test
    void shouldRejectDuplicateManualJiraIntegration() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);

        TenantContext.setTenantId("tenant-123");
        when(repository.findByJiraWebhookId("webhook-1")).thenReturn(Optional.of(new JiraIntegration()));

        assertThatThrownBy(() -> service.create("webhook-1", "secret-abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verifyNoInteractions(encryptionService);
        verify(repository, never()).save(any(JiraIntegration.class));
    }

    @Test
    void shouldCreateOAuthJiraIntegration() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);

        TenantContext.setTenantId("tenant-123");
        when(repository.findByJiraWebhookId("webhook-1")).thenReturn(Optional.empty());
        when(encryptionService.encrypt("access-token")).thenReturn("enc-access");
        when(encryptionService.encrypt("refresh-token")).thenReturn("enc-refresh");
        when(repository.save(any(JiraIntegration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JiraIntegration integration = service.createFromOAuth(
                "cloud-123",
                "https://mycompany.atlassian.net",
                "My Jira",
                "access-token",
                "refresh-token",
                "webhook-1"
        );

        assertThat(integration.getTenantId()).isEqualTo("tenant-123");
        assertThat(integration.getJiraWebhookId()).isEqualTo("webhook-1");
        assertThat(integration.getIntegrationType()).isEqualTo(JiraIntegrationType.OAUTH);
        assertThat(integration.getCloudId()).isEqualTo("cloud-123");
        assertThat(integration.getSiteUrl()).isEqualTo("https://mycompany.atlassian.net");
        assertThat(integration.getSiteName()).isEqualTo("My Jira");
        assertThat(integration.getAccessTokenEncrypted()).isEqualTo("enc-access");
        assertThat(integration.getRefreshTokenEncrypted()).isEqualTo("enc-refresh");
        verify(repository).save(any(JiraIntegration.class));
    }

    @Test
    void shouldSkipRefreshTokenEncryptionWhenBlank() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);

        TenantContext.setTenantId("tenant-123");
        when(repository.findByJiraWebhookId("webhook-1")).thenReturn(Optional.empty());
        when(encryptionService.encrypt("access-token")).thenReturn("enc-access");
        when(repository.save(any(JiraIntegration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JiraIntegration integration = service.createFromOAuth(
                "cloud-123",
                "https://mycompany.atlassian.net",
                "My Jira",
                "access-token",
                "   ",
                "webhook-1"
        );

        assertThat(integration.getAccessTokenEncrypted()).isEqualTo("enc-access");
        assertThat(integration.getRefreshTokenEncrypted()).isNull();
        verify(encryptionService).encrypt("access-token");
    }

    @Test
    void shouldRejectInvalidOAuthInput() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);

        TenantContext.setTenantId("tenant-123");

        assertThatThrownBy(() -> service.createFromOAuth(null, "https://example.com", "My Jira", "token", "refresh", "webhook-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cloud ID");

        assertThatThrownBy(() -> service.createFromOAuth("cloud-123", " ", "My Jira", "token", "refresh", "webhook-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("site URL");

        assertThatThrownBy(() -> service.createFromOAuth("cloud-123", "https://example.com", "My Jira", null, "refresh", "webhook-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("access token");

        assertThatThrownBy(() -> service.createFromOAuth("cloud-123", "https://example.com", "My Jira", "token", "refresh", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("webhook ID");
    }
}
