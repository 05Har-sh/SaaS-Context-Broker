package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.config.slackoauth.SlackOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.SlackOAuthAccessResponse;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class SlackIntegrationService {
    private final SlackIntegrationRepository repository;
    private final CredentialEncryptionService encryptionService;
    private final SlackOAuthProperties oauthProperties;

    public SlackIntegrationService(SlackIntegrationRepository repository, CredentialEncryptionService encryptionService, SlackOAuthProperties oauthProperties) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.oauthProperties = oauthProperties;
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

    public SlackIntegration createFromOAuth(
            String tenantId,
            SlackOAuthAccessResponse response
    ) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant ID cannot be null or blank"
            );
        }

        if (response == null || !response.ok()) {
            throw new IllegalArgumentException(
                    "Invalid Slack OAuth response"
            );
        }

        if (response.team() == null
                || response.team().id() == null
                || response.team().id().isBlank()) {
            throw new IllegalArgumentException(
                    "Slack workspace information is missing"
            );
        }

        if (response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new IllegalArgumentException(
                    "Slack access token is missing"
            );
        }

        String teamId = response.team().id();

        if (repository.findByTeamId(teamId).isPresent()) {
            throw new IllegalArgumentException(
                    "Slack Integration already exists"
            );
        }

        SlackIntegration integration = new SlackIntegration();

        integration.setTenantId(tenantId);
        integration.setTeamId(teamId);
        integration.setTeamName(response.team().name());
        integration.setBotUserId(response.botUserId());

        integration.setSigningSecret(encryptionService.encrypt(oauthProperties.getSigningSecret()));

        integration.setAccessTokenEncrypted(encryptionService.encrypt(response.accessToken()));

        /*
         * Signing secret is intentionally NOT handled here yet.
         *
         * OAuth does not return the Slack signing secret.
         */

        return repository.save(integration);
    }
}
