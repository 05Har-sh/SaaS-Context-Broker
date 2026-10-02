package com.harsh.context_broker.contextBroker.service.oauth;

import com.harsh.context_broker.contextBroker.client.AtlassianOAuthClient;
import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianAccessibleResourceResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianOAuthTokenResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationRequest;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationResponse;
import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
import com.harsh.context_broker.contextBroker.oauth.jirastate.JiraOAuthStateService;
import com.harsh.context_broker.contextBroker.service.JiraIntegrationService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JiraOAuthServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private JiraOAuthService newService(JiraOAuthProperties properties,
                                       JiraOAuthStateService stateService,
                                       AtlassianOAuthClient oauthClient,
                                       JiraIntegrationService integrationService) {
        return new JiraOAuthService(properties, stateService, oauthClient, integrationService);
    }

    @Test
    void shouldCreateAuthorizationUrlUsingTenantContextAndState() {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        properties.setClientId("jira-client-id");
        properties.setScopes("read:jira-work write:jira-work");
        properties.setRedirectUri("https://example.com/jira/callback");

        JiraOAuthStateService stateService = mock(JiraOAuthStateService.class);
        when(stateService.createState("tenant-123", "user-456")).thenReturn("state-abc");

        JiraOAuthService service = newService(properties, stateService, mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));
        TenantContext.setTenantId("tenant-123");

        String authorizationUrl = service.createAuthorizationUrl(
                new UsernamePasswordAuthenticationToken("user-456", null)
        );

        assertThat(authorizationUrl)
                .startsWith("https://auth.atlassian.com/authorize")
                .contains("audience=api.atlassian.com")
                .contains("client_id=jira-client-id")
                .contains("scope=read:jira-work%20write:jira-work")
                .contains("redirect_uri=https://example.com/jira/callback")
                .contains("state=state-abc")
                .contains("response_type=code")
                .contains("prompt=consent");

        verify(stateService).createState("tenant-123", "user-456");
    }

    @Test
    void shouldRequireTenantContextToCreateAuthorizationUrl() {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        properties.setClientId("jira-client-id");
        properties.setScopes("read:jira-work");
        properties.setRedirectUri("https://example.com/jira/callback");

        JiraOAuthService service = newService(properties, mock(JiraOAuthStateService.class), mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));

        assertThatThrownBy(() -> service.createAuthorizationUrl(
                new UsernamePasswordAuthenticationToken("user-456", null)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context available");
    }

    @Test
    void shouldExchangeJiraAuthorizationCode() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthProperties properties = new JiraOAuthProperties();
        JiraOAuthStateService stateService = mock(JiraOAuthStateService.class);
        JiraIntegrationService integrationService = mock(JiraIntegrationService.class);
        JiraOAuthService service = newService(properties, stateService, oauthClient, integrationService);

        AtlassianOAuthTokenResponse response = new AtlassianOAuthTokenResponse(
                "access-token",
                3600L,
                "read:jira-work write:jira-work",
                "refresh-token"
        );

        when(oauthClient.exchangeCode("test-code")).thenReturn(response);

        AtlassianOAuthTokenResponse result = service.exchangeCode("test-code");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.expiresIn()).isEqualTo(3600L);
        assertThat(result.refreshToken()).isEqualTo("refresh-token");

        verify(oauthClient).exchangeCode("test-code");
    }

    @Test
    void shouldRejectJiraOAuthResponseWithoutAccessToken() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        AtlassianOAuthTokenResponse response = new AtlassianOAuthTokenResponse(
                null,
                null,
                null,
                null
        );

        when(oauthClient.exchangeCode("bad-code")).thenReturn(response);

        assertThatThrownBy(() -> service.exchangeCode("bad-code"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void shouldRejectMissingJiraAuthorizationCode() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        assertThatThrownBy(() -> service.exchangeCode(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");

        verifyNoInteractions(oauthClient);
    }

    @Test
    void shouldGetAccessibleResources() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService jiraOAuthService = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        List<AtlassianAccessibleResourceResponse> resources = List.of(
                new AtlassianAccessibleResourceResponse(
                        "cloud-123",
                        "My Jira",
                        "https://mycompany.atlassian.net",
                        List.of("read:jira-work"),
                        null
                )
        );

        when(oauthClient.getAccessibleResources("access-token")).thenReturn(resources);

        List<AtlassianAccessibleResourceResponse> result = jiraOAuthService.getAccessibleResources("access-token");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("cloud-123");
        assertThat(result.get(0).name()).isEqualTo("My Jira");

        verify(oauthClient).getAccessibleResources("access-token");
    }

    @Test
    void shouldRejectMissingAccessTokenForAccessibleResources() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService jiraOAuthService = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        assertThatThrownBy(() -> jiraOAuthService.getAccessibleResources(null))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(oauthClient);
    }

    @Test
    void shouldRegisterWebhooks() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService jiraOAuthService = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        JiraWebhookRegistrationRequest request = new JiraWebhookRegistrationRequest(
                "https://example.com/webhook",
                List.of(new JiraWebhookRegistrationRequest.WebhookDetails("project = KEY", List.of("issue_created")))
        );

        List<JiraWebhookRegistrationResponse> response = List.of(
                new JiraWebhookRegistrationResponse(42L, List.of())
        );

        when(oauthClient.registerWebhooks("cloud-123", "access-token", request)).thenReturn(response);

        List<JiraWebhookRegistrationResponse> result = jiraOAuthService.registerWebhooks("cloud-123", "access-token", request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).createdWebhookId()).isEqualTo(42L);
        verify(oauthClient).registerWebhooks("cloud-123", "access-token", request);
    }

    @Test
    void shouldRejectMissingCloudIdForWebhookRegistration() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService jiraOAuthService = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        JiraWebhookRegistrationRequest request = new JiraWebhookRegistrationRequest(
                "https://example.com/webhook",
                List.of(new JiraWebhookRegistrationRequest.WebhookDetails("project = KEY", List.of("issue_created")))
        );

        assertThatThrownBy(() -> jiraOAuthService.registerWebhooks(null, "access-token", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cloud ID");

        verifyNoInteractions(oauthClient);
    }

    @Test
    void shouldRejectMissingAccessTokenForWebhookRegistration() {
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraOAuthService jiraOAuthService = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), oauthClient, mock(JiraIntegrationService.class));

        JiraWebhookRegistrationRequest request = new JiraWebhookRegistrationRequest(
                "https://example.com/webhook",
                List.of(new JiraWebhookRegistrationRequest.WebhookDetails("project = KEY", List.of("issue_created")))
        );

        assertThatThrownBy(() -> jiraOAuthService.registerWebhooks("cloud-123", null, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("access token");

        verifyNoInteractions(oauthClient);
    }

    @Test
    void shouldSelectSingleAccessibleResource() {
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));
        AtlassianAccessibleResourceResponse resource = new AtlassianAccessibleResourceResponse(
                "cloud-123", "My Jira", "https://mycompany.atlassian.net", List.of("read:jira-work"), null
        );

        assertThat(service.selectResource(List.of(resource))).isEqualTo(resource);
    }

    @Test
    void shouldRejectEmptyAccessibleResourcesSelection() {
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));

        assertThatThrownBy(() -> service.selectResource(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No accessible Jira sites found");
    }

    @Test
    void shouldRejectMultipleAccessibleResourcesSelection() {
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));
        AtlassianAccessibleResourceResponse first = new AtlassianAccessibleResourceResponse(
                "cloud-1", "One", "https://one.atlassian.net", List.of("read:jira-work"), null
        );
        AtlassianAccessibleResourceResponse second = new AtlassianAccessibleResourceResponse(
                "cloud-2", "Two", "https://two.atlassian.net", List.of("read:jira-work"), null
        );

        assertThatThrownBy(() -> service.selectResource(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple Jira sites are accessible");
    }

    @Test
    void shouldHandleSuccessfulCallbackAndCreateIntegration() {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        properties.setWebhookUrl("https://example.com/jira/webhook");

        JiraOAuthStateService stateService = mock(JiraOAuthStateService.class);
        AtlassianOAuthClient oauthClient = mock(AtlassianOAuthClient.class);
        JiraIntegrationService integrationService = mock(JiraIntegrationService.class);
        JiraOAuthService service = newService(properties, stateService, oauthClient, integrationService);

        when(stateService.consumeState("state-abc")).thenReturn(new OAuthStateData("tenant-123", "user-456"));
        when(oauthClient.exchangeCode("code-123")).thenReturn(new AtlassianOAuthTokenResponse("access-token", 3600L, "read:jira-work", "refresh-token"));

        AtlassianAccessibleResourceResponse resource = new AtlassianAccessibleResourceResponse(
                "cloud-123", "My Jira", "https://mycompany.atlassian.net", List.of("read:jira-work"), null
        );
        when(oauthClient.getAccessibleResources("access-token")).thenReturn(List.of(resource));

        JiraWebhookRegistrationResponse webhookResponse = new JiraWebhookRegistrationResponse(987L, List.of());
        when(oauthClient.registerWebhooks(eq("cloud-123"), eq("access-token"), any(JiraWebhookRegistrationRequest.class)))
                .thenReturn(List.of(webhookResponse));

        JiraIntegration integration = new JiraIntegration();
        when(integrationService.createFromOAuth("cloud-123", "https://mycompany.atlassian.net", "My Jira", "access-token", "refresh-token", "987")).thenReturn(integration);

        service.handleCallback("code-123", "state-abc");

        assertThat(TenantContext.getTenantId()).isNull();
        verify(stateService).consumeState("state-abc");
        verify(oauthClient).exchangeCode("code-123");
        verify(oauthClient).getAccessibleResources("access-token");
        verify(oauthClient).registerWebhooks(eq("cloud-123"), eq("access-token"), any(JiraWebhookRegistrationRequest.class));
        verify(integrationService).createFromOAuth("cloud-123", "https://mycompany.atlassian.net", "My Jira", "access-token", "refresh-token", "987");
    }

    @Test
    void shouldRejectMissingAuthorizationCodeInCallback() {
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));

        assertThatThrownBy(() -> service.handleCallback(null, "state-abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("authorization code");
    }

    @Test
    void shouldRejectMissingStateInCallback() {
        JiraOAuthService service = newService(new JiraOAuthProperties(), mock(JiraOAuthStateService.class), mock(AtlassianOAuthClient.class), mock(JiraIntegrationService.class));

        assertThatThrownBy(() -> service.handleCallback("code-123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OAuth state");
    }
}

