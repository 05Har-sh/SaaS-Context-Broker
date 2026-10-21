package com.harsh.context_broker.contextBroker.controller.integration;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.model.JiraConnectionStatus;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.service.JiraIntegrationService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JiraIntegrationControllerTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnOnlyCurrentTenantConnections() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);
        JiraIntegrationController controller = new JiraIntegrationController(service);

        TenantContext.setTenantId("tenant-42");
        JiraIntegration integration = new JiraIntegration();
        integration.setId(99L);
        integration.setTenantId("tenant-42");
        integration.setCloudId("cloud-42");
        integration.setSiteName("Tenant Jira");
        integration.setSiteUrl("https://tenant.atlassian.net");
        integration.setConnectionStatus(JiraConnectionStatus.CONNECTED);
        integration.setJiraWebhookId("hook-42");
        integration.setIntegrationType(JiraIntegrationType.OAUTH);
        integration.setCreatedAt(OffsetDateTime.now());
        integration.setUpdatedAt(OffsetDateTime.now());
        when(repository.findAllByTenantIdOrderByCreatedAtDesc("tenant-42")).thenReturn(List.of(integration));

        ResponseEntity<Map<String, Object>> response = controller.status();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("connected", true);
        assertThat((List<?>) response.getBody().get("integrations")).hasSize(1);
    }

    @Test
    void shouldRejectDisconnectForAnotherTenant() {
        JiraIntegrationRepository repository = mock(JiraIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        JiraIntegrationService service = new JiraIntegrationService(repository, encryptionService);

        TenantContext.setTenantId("tenant-42");
        when(repository.findByIdAndTenantId(99L, "tenant-42")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disconnect(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
