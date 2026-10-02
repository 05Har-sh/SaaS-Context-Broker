package com.harsh.context_broker.contextBroker.service.oauth;

import com.harsh.context_broker.contextBroker.client.SlackOAuthClient;
import com.harsh.context_broker.contextBroker.config.slackoauth.SlackOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.SlackOAuthAccessResponse;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
import com.harsh.context_broker.contextBroker.oauth.slackstate.OAuthStateService;
import com.harsh.context_broker.contextBroker.service.SlackIntegrationService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SlackOAuthService {
    private final OAuthStateService stateService;
    private final SlackOAuthProperties properties;
    private final SlackOAuthClient slackOAuthClient;
    private final SlackIntegrationService slackIntegrationService;

    public SlackOAuthService(OAuthStateService stateService, SlackOAuthProperties properties, SlackOAuthClient slackOAuthClient, SlackIntegrationService slackIntegrationService) {
        this.stateService = stateService;
        this.properties = properties;
        this.slackOAuthClient = slackOAuthClient;
        this.slackIntegrationService = slackIntegrationService;
    }

    public String createAuthorizationUrl(Authentication authentication) {
        String tenantId = TenantContext.requireTenantId();
        String userId = authentication.getName();
        String state = stateService.createState(tenantId, userId);

        return UriComponentsBuilder.fromUriString("https://slack.com/oauth/v2/authorize")
                .queryParam("client_id", properties.getClientId())
                .queryParam("scope", properties.getScopes())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }
    public SlackOAuthAccessResponse exchangeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Slack authorization code is missing"
            );
        }
        SlackOAuthAccessResponse response = slackOAuthClient.exchangeCode(code);
        if (!response.ok()) {
            throw new IllegalStateException(
                    "Slack OAuth exchange failed: " + response.error()
            );
        }
        return response;
    }
    public SlackIntegration handleCallback(String code, String state) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Slack authorization code is missing"
            );
        }

        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException(
                    "Slack OAuth state is missing"
            );
        }
        OAuthStateData stateData = stateService.consumeState(state);
        SlackOAuthAccessResponse response = slackOAuthClient.exchangeCode(code);

        if (!response.ok()) {
            throw new IllegalStateException(
                    "Slack OAuth exchange failed: "
            );
        }
        return slackIntegrationService.createFromOAuth(stateData.tenantId(), response);

//        return new SlackOAuthCallbackResult(
//                stateData.tenantId(),
//                stateData.userId(),
//                response
//        );
    }
}
