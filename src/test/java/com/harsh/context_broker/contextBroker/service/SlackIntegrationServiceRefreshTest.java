package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.config.slackoauth.SlackOAuthProperties;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackIntegrationServiceRefreshTest {

    @Test
    void refreshSigningSecretUpdatesPersistedIntegration() {
        SlackIntegrationRepository repository = mock(SlackIntegrationRepository.class);
        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
        SlackOAuthProperties properties = new SlackOAuthProperties();
        properties.setSigningSecret("app-secret");

        SlackIntegrationService service = new SlackIntegrationService(repository, encryptionService, properties);

        SlackIntegration existing = new SlackIntegration();
        existing.setTeamId("T123");
        existing.setTenantId("tenant-1");

        when(repository.findByTeamId("T123")).thenReturn(Optional.of(existing));
        when(encryptionService.encrypt("fresh-secret")).thenReturn("encrypted-fresh-secret");
        when(repository.save(existing)).thenReturn(existing);

        SlackIntegration updated = service.updateSigningSecret("T123", "fresh-secret");

        assertThat(updated.getSigningSecret()).isEqualTo("encrypted-fresh-secret");
        verify(repository).save(existing);
    }
}
