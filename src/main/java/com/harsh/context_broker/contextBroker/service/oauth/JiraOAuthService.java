package com.harsh.context_broker.contextBroker.service.oauth;

import com.harsh.context_broker.contextBroker.client.AtlassianOAuthClient;
import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianAccessibleResourceResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianOAuthTokenResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationRequest;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationResponse;
import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
import com.harsh.context_broker.contextBroker.oauth.jirastate.JiraOAuthStateService;
import com.harsh.context_broker.contextBroker.service.JiraIntegrationService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class JiraOAuthService {
    private final JiraOAuthProperties properties;
    private final JiraOAuthStateService stateService;
    private final AtlassianOAuthClient oauthClient;
    private final JiraIntegrationService jiraIntegrationService;


    public JiraOAuthService(JiraOAuthProperties properties, JiraOAuthStateService stateService, AtlassianOAuthClient oauthClient, JiraIntegrationService jiraIntegrationService) {
        this.properties = properties;
        this.stateService = stateService;
        this.oauthClient = oauthClient;
        this.jiraIntegrationService = jiraIntegrationService;
    }
    public String createAuthorizationUrl(Authentication authentication) {
        String tenantId = TenantContext.requireTenantId();
        String userId = authentication.getName();
        String state = stateService.createState(tenantId, userId);

        return UriComponentsBuilder.fromUriString("https://auth.atlassian.com/authorize")
                .queryParam("audience", "api.atlassian.com")
                .queryParam("client_id", properties.getClientId())
                .queryParam("scope", properties.getScopes())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();
    }
    public AtlassianOAuthTokenResponse exchangeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Jira authorization code is missing");
        }
        AtlassianOAuthTokenResponse response = oauthClient.exchangeCode(code);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Atlassian OAuth response does not contain an access token");
        }
        return response;
    }

    public List<AtlassianAccessibleResourceResponse> getAccessibleResources(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira access token is missing"
            );
        }
        return oauthClient.getAccessibleResources(accessToken);
    }
    public List<JiraWebhookRegistrationResponse> registerWebhooks(
            String cloudId,
            String accessToken,
            JiraWebhookRegistrationRequest request
    ) {
        if (cloudId == null || cloudId.isBlank()) {
            throw new IllegalArgumentException("Jira cloud ID is missing");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Jira access token is missing");
        }

        return oauthClient.registerWebhooks(
                cloudId,
                accessToken,
                request
        );
    }
    public AtlassianAccessibleResourceResponse selectResource(List<AtlassianAccessibleResourceResponse> resources) {
        if (resources == null || resources.isEmpty()) {
            throw new IllegalStateException(
                    "No accessible Jira sites found"
            );
        }

        if (resources.size() > 1) {
            throw new IllegalStateException(
                    "Multiple Jira sites are accessible; site selection is required"
            );
        }

        return resources.get(0);
    }
    public void handleCallback(String code, String state) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira authorization code is missing"
            );
        }

        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira OAuth state is missing"
            );
        }

        OAuthStateData stateData =
                stateService.consumeState(state);

        TenantContext.setTenantId(stateData.tenantId());

        try {
            AtlassianOAuthTokenResponse token =
                    exchangeCode(code);

            List<AtlassianAccessibleResourceResponse> resources =
                    getAccessibleResources(token.accessToken());

            if (resources.isEmpty()) {
                throw new IllegalStateException(
                        "No accessible Jira sites found"
                );
            }

            if (resources.size() > 1) {
                throw new IllegalStateException(
                        "Multiple Jira sites are accessible; site selection is required"
                );
            }

            AtlassianAccessibleResourceResponse resource =
                    resources.get(0);

            JiraWebhookRegistrationRequest webhookRequest =
                    new JiraWebhookRegistrationRequest(
                            properties.getWebhookUrl(),
                            List.of(
                                    new JiraWebhookRegistrationRequest.WebhookDetails(
                                            "",
                                            List.of(
                                                    "jira:issue_created",
                                                    "jira:issue_updated",
                                                    "jira:issue_deleted"
                                            )
                                    )
                            )
                    );

            List<JiraWebhookRegistrationResponse> webhookResponses =
                    registerWebhooks(
                            resource.id(),
                            token.accessToken(),
                            webhookRequest
                    );

            if (webhookResponses.isEmpty()
                    || webhookResponses.get(0).createdWebhookId() == null) {

                throw new IllegalStateException(
                        "Jira webhook registration failed"
                );
            }

            Long webhookId =
                    webhookResponses.get(0).createdWebhookId();

            jiraIntegrationService.createFromOAuth(
                    resource.id(),
                    resource.url(),
                    resource.name(),
                    token.accessToken(),
                    token.refreshToken(),
                    webhookId.toString()
            );

        } finally {
            TenantContext.clear();
        }
    }
}
