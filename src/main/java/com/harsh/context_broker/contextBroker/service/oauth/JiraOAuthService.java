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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class JiraOAuthService {
    private static final Logger log = LoggerFactory.getLogger(JiraOAuthService.class);
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
        String userId = authentication != null ? authentication.getName() : "unknown";
        log.info("Creating Jira OAuth authorization URL: tenantId={}, userId={}, redirectUri={}, clientIdConfigured={}",
                tenantId,
                userId,
                properties.getRedirectUri(),
                properties.getClientId() != null && !properties.getClientId().isBlank());
        String state = stateService.createState(tenantId, userId);

        String authorizationUrl = UriComponentsBuilder.fromUriString("https://auth.atlassian.com/authorize")
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

        log.debug("Generated Jira OAuth state for tenantId={}, state={}", tenantId, state);
        return authorizationUrl;
    }
    public AtlassianOAuthTokenResponse exchangeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Jira authorization code is missing");
        }
        log.info("Exchanging Jira authorization code with Atlassian: codeLength={}, redirectUri={}, clientIdConfigured={}",
                code.length(),
                properties.getRedirectUri(),
                properties.getClientId() != null && !properties.getClientId().isBlank());
        AtlassianOAuthTokenResponse response = oauthClient.exchangeCode(code);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.error("Jira OAuth token exchange failed: responseWasNull={}, accessTokenPresent={}", response == null, response != null && response.accessToken() != null && !response.accessToken().isBlank());
            throw new IllegalStateException("Atlassian OAuth response does not contain an access token");
        }
        log.info("Jira OAuth token exchange succeeded: expiresIn={}, scopesPresent={}, refreshTokenPresent={}",
                response.expiresIn(),
                response.scope() != null && !response.scope().isBlank(),
                response.refreshToken() != null && !response.refreshToken().isBlank());
        return response;
    }

    public List<AtlassianAccessibleResourceResponse> getAccessibleResources(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Jira access token is missing"
            );
        }
        log.info("Fetching accessible Jira resources for access token length={}", accessToken.length());
        List<AtlassianAccessibleResourceResponse> resources = oauthClient.getAccessibleResources(accessToken);
        log.info("Accessible Jira resources response received: count={}", resources == null ? 0 : resources.size());
        return resources;
    }
    public JiraWebhookRegistrationResponse registerWebhooks(
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

        log.info("Registering Jira webhook for cloudId={}, webhookTarget={}, eventCount={}",
                cloudId,
                request != null ? request.url() : "unknown",
                request != null && request.webhooks() != null ? request.webhooks().size() : 0);
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

        log.info("Handling Jira OAuth callback: codeLength={}, stateLength={}", code.length(), state.length());

        OAuthStateData stateData =
                stateService.consumeState(state);
        log.info("Consumed Jira OAuth state: tenantId={}, userId={}", stateData.tenantId(), stateData.userId());

        TenantContext.setTenantId(stateData.tenantId());

        try {
            AtlassianOAuthTokenResponse token =
                    exchangeCode(code);

            List<AtlassianAccessibleResourceResponse> resources =
                    getAccessibleResources(token.accessToken());

            if (resources == null || resources.isEmpty()) {
                log.error("Jira OAuth callback failed: Atlassian returned no accessible Jira resources for tenantId={}", stateData.tenantId());
                throw new IllegalStateException(
                        "No accessible Jira sites found"
                );
            }

            if (resources.size() > 1) {
                log.error("Jira OAuth callback failed: multiple Jira sites returned for tenantId={}, siteCount={}", stateData.tenantId(), resources.size());
                throw new IllegalStateException(
                        "Multiple Jira sites are accessible; site selection is required"
                );
            }

            AtlassianAccessibleResourceResponse resource =
                    resources.get(0);
            log.info("Selected Jira site for OAuth: cloudId={}, name={}, url={}", resource.id(), resource.name(), resource.url());

            if (properties.getWebhookUrl() == null || properties.getWebhookUrl().isBlank()) {
                throw new IllegalStateException("Jira webhook URL is missing");
            }

            String webhookJql = properties.getWebhookJql() != null && !properties.getWebhookJql().isBlank()
                    ? properties.getWebhookJql()
                    : "project = ABC";

            JiraWebhookRegistrationRequest webhookRequest =
                    new JiraWebhookRegistrationRequest(
                            properties.getWebhookUrl(),
                            List.of(
                                    new JiraWebhookRegistrationRequest.WebhookDetails(
                                            webhookJql,
                                            List.of(
                                                    "jira:issue_created",
                                                    "jira:issue_updated",
                                                    "jira:issue_deleted"
                                            )
                                    )
                            )
                    );

            JiraWebhookRegistrationResponse webhookResponse =
                    registerWebhooks(
                            resource.id(),
                            token.accessToken(),
                            webhookRequest
                    );

            Long webhookId = null;
            String webhookErrorMessage = null;
            if (webhookResponse == null || webhookResponse.webhookRegistrationResult() == null
                    || webhookResponse.webhookRegistrationResult().isEmpty()) {
                log.error("Jira OAuth callback failed: webhook registration response was empty for cloudId={}, webhookTarget={}",
                        resource.id(),
                        properties.getWebhookUrl());
                throw new IllegalStateException("Jira webhook registration failed: response was empty");
            }

            for (JiraWebhookRegistrationResponse.WebhookResult registrationResult : webhookResponse.webhookRegistrationResult()) {
                if (registrationResult.createdWebhookId() != null) {
                    webhookId = registrationResult.createdWebhookId();
                    break;
                }
                if (registrationResult.errors() != null && !registrationResult.errors().isEmpty()) {
                    webhookErrorMessage = String.join("; ", registrationResult.errors());
                    log.error("Jira webhook registration returned per-webhook errors for cloudId={}, errors={}",
                            resource.id(),
                            registrationResult.errors());
                }
            }

            if (webhookId == null) {
                log.error("Jira OAuth callback failed: webhook registration returned no createdWebhookId for cloudId={}, webhookTarget={}, errors={}",
                        resource.id(),
                        properties.getWebhookUrl(),
                        webhookErrorMessage);
                throw new IllegalStateException(
                        webhookErrorMessage == null
                                ? "Jira webhook registration failed"
                                : "Jira webhook registration failed: " + webhookErrorMessage
                );
            }

            log.info("Jira webhook registration succeeded: cloudId={}, createdWebhookId={}", resource.id(), webhookId);

            jiraIntegrationService.createFromOAuth(
                    resource.id(),
                    resource.url(),
                    resource.name(),
                    token.accessToken(),
                    token.refreshToken(),
                    webhookId.toString()
            );
            log.info("Jira integration created successfully for tenantId={}, cloudId={}", stateData.tenantId(), resource.id());

        } finally {
            TenantContext.clear();
        }
    }
}
