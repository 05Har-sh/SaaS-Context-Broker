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

public SlackIntegration getByTeamIdAndTenant(String teamId, String tenantId) {
       return repository.findByTeamIdAndTenantId(teamId, tenantId)
               .orElseThrow(() -> new IllegalArgumentException("Slack workspace not associated with this tenant"));
}

public SlackIntegration getCurrentTenantIntegration() {
       String tenantId = TenantContext.requireTenantId();
       return repository.findAllByTenantId(tenantId).stream()
               .findFirst()
               .orElse(null);
}

public SlackIntegration create(String teamId, String signingSecret) {
       String tenantId = TenantContext.requireTenantId();

       if (teamId == null || teamId.isBlank()) {
           throw new IllegalArgumentException("Slack team ID cannot be blank");
       }
       if (signingSecret == null || signingSecret.isBlank()) {
           throw new IllegalArgumentException("Slack signing secret cannot be blank");
       }

       repository.findByTeamId(teamId).ifPresent(existing -> {
           if (!existing.getTenantId().equals(tenantId)) {
               throw new IllegalArgumentException("Slack workspace already connected to another tenant");
           }
           throw new IllegalStateException("Slack workspace is already connected for this tenant");
       });

       SlackIntegration integration = new SlackIntegration();
       integration.setTenantId(tenantId);
       integration.setTeamId(teamId);
       integration.setSigningSecret(encryptionService.encrypt(signingSecret));
       integration.setActive(true);

       return repository.save(integration);
}

public SlackIntegration updateSigningSecret(String teamId, String signingSecret) {
       if (teamId == null || teamId.isBlank()) {
           throw new IllegalArgumentException("Slack team ID cannot be null or blank");
       }
       if (signingSecret == null || signingSecret.isBlank()) {
           throw new IllegalArgumentException("Slack signing secret cannot be null or blank");
       }

       SlackIntegration integration = repository.findByTeamId(teamId)
               .orElseThrow(() -> new IllegalArgumentException("Unknown Slack team"));

       integration.setSigningSecret(encryptionService.encrypt(signingSecret));
       return repository.save(integration);
}

public SlackIntegration disconnect(Long integrationId) {
       String tenantId = TenantContext.requireTenantId();
       SlackIntegration integration = repository.findByIdAndTenantId(integrationId, tenantId)
               .orElseThrow(() -> new IllegalArgumentException("Slack integration not found for this tenant"));
       integration.setActive(false);
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
       SlackIntegration existing = repository.findByTeamId(teamId).orElse(null);

       if (existing != null && !existing.getTenantId().equals(tenantId)) {
           throw new IllegalArgumentException(
                   "Slack workspace already connected to another tenant"
           );
       }

       if (existing != null) {
           existing.setTenantId(tenantId);
           existing.setTeamName(response.team().name());
           existing.setBotUserId(response.botUserId());
           existing.setSigningSecret(encryptionService.encrypt(oauthProperties.getSigningSecret()));
           existing.setAccessTokenEncrypted(encryptionService.encrypt(response.accessToken()));
           existing.setActive(true);
           return repository.save(existing);
       }

       SlackIntegration integration = new SlackIntegration();

       integration.setTenantId(tenantId);
       integration.setTeamId(teamId);
       integration.setTeamName(response.team().name());
       integration.setBotUserId(response.botUserId());
       integration.setSigningSecret(encryptionService.encrypt(oauthProperties.getSigningSecret()));
       integration.setAccessTokenEncrypted(encryptionService.encrypt(response.accessToken()));
       integration.setActive(true);

       return repository.save(integration);
}
}
