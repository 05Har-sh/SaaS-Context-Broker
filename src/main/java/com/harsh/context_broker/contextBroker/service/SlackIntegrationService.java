package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class SlackIntegrationService {
    private final SlackIntegrationRepository repository;
    private final CredentialEncryptionService encryptionService;

    public SlackIntegrationService(SlackIntegrationRepository repository, CredentialEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }
    public SlackIntegration getByTeamId(String teamId) {
        return repository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Slack team"));
    }

    /*
JWT
 ↓
TenantContext
 ↓
tenantId
 ↓
create SlackIntegration
 ↓
encrypt signingSecret
 ↓
PostgreSQL
     */

    public SlackIntegration create(String teamId, String signingSecret) {
        String tenantId = TenantContext.requireTenantId();
        if(repository.findByTeamId(teamId).isPresent()) {
            throw new IllegalArgumentException("Slack Integration already exists");
        }
        SlackIntegration integration = new SlackIntegration();
        integration.setTenantId(tenantId);
        integration.setTeamId(teamId);
        integration.setSigningSecret(encryptionService.encrypt(signingSecret));

        return repository.save(integration);
    }
}
