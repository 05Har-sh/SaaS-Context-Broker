package com.harsh.context_broker.contextBroker.client;

import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianAccessibleResourceResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianOAuthTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtlassianOAuthClientFullTest {

    @Test
    void shouldExchangeCodeSuccessfully() throws Exception {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        properties.setClientId("cid");
        properties.setClientSecret("secret");
        properties.setRedirectUri("https://app/callback");

        RestClient mockAuth = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        RestClient mockApi = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        AtlassianOAuthTokenResponse response = new AtlassianOAuthTokenResponse("access-token", 3600L, "scope", "refresh");

        when(mockAuth.post()
                .uri(eq("/oauth/token"))
                .contentType(any(org.springframework.http.MediaType.class))
                .body(any(java.util.Map.class))
                .retrieve()
                .body(AtlassianOAuthTokenResponse.class))
                .thenReturn(response);

        AtlassianOAuthClient client = new AtlassianOAuthClient(properties, mockAuth, mockApi);

        AtlassianOAuthTokenResponse result = client.exchangeCode("test-code");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.expiresIn()).isEqualTo(3600L);
        assertThat(result.refreshToken()).isEqualTo("refresh");

        verify(mockAuth.post()).uri(eq("/oauth/token"));
    }

    @Test
    void shouldRejectEmptyExchangeResponse() throws Exception {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        properties.setClientId("cid");
        properties.setClientSecret("secret");
        properties.setRedirectUri("https://app/callback");

        RestClient mockAuth = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        RestClient mockApi = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(mockAuth.post()
                .uri(eq("/oauth/token"))
                .contentType(any(org.springframework.http.MediaType.class))
                .body(any(java.util.Map.class))
                .retrieve()
                .body(AtlassianOAuthTokenResponse.class))
                .thenReturn(null);

        AtlassianOAuthClient client = new AtlassianOAuthClient(properties, mockAuth, mockApi);

        assertThatThrownBy(() -> client.exchangeCode("code"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Atlassian OAuth returned an empty response");
    }

    @Test
    void shouldRejectMissingAuthorizationCode() {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        AtlassianOAuthClient client = new AtlassianOAuthClient(properties);

        assertThatThrownBy(() -> client.exchangeCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization code is missing");
    }

    @Test
    void shouldGetAccessibleResourcesSuccessfully() throws Exception {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        RestClient mockApi = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        RestClient mockAuth = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        List<AtlassianAccessibleResourceResponse> list = List.of(
                new AtlassianAccessibleResourceResponse("id", "name", "baseUrl", List.of("scopes"), "avatar")
        );

        // prepare the chained spec returned by get().uri(...)
        RestClient.RequestHeadersSpec<?> spec = mockApi.get().uri("/oauth/token/accessible-resources");
        // header is a varargs method; ensure it returns the same spec so chaining works
        org.mockito.Mockito.doReturn(spec).when(spec).header(any(String.class), any());
        when(spec.retrieve()
                .body(any(ParameterizedTypeReference.class)))
                .thenReturn(list);

        AtlassianOAuthClient client = new AtlassianOAuthClient(properties, mockAuth, mockApi);

        List<AtlassianAccessibleResourceResponse> result = client.getAccessibleResources("token123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("id");
    }

    @Test
    void shouldRejectMissingAccessTokenForResources() {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        AtlassianOAuthClient client = new AtlassianOAuthClient(properties);

        assertThatThrownBy(() -> client.getAccessibleResources(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access token is missing");
    }

    @Test
    void shouldRejectEmptyAccessibleResourcesResponse() throws Exception {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        RestClient mockApi = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        RestClient mockAuth = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        RestClient.RequestHeadersSpec<?> spec = mockApi.get().uri("/oauth/token/accessible-resources");
        org.mockito.Mockito.doReturn(spec).when(spec).header(any(String.class), any());
        when(spec.retrieve()
                .body(any(ParameterizedTypeReference.class)))
                .thenReturn(null);

        AtlassianOAuthClient client = new AtlassianOAuthClient(properties, mockAuth, mockApi);

        assertThatThrownBy(() -> client.getAccessibleResources("token123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Atlassian accessible-resources returned an empty response");
    }
}
