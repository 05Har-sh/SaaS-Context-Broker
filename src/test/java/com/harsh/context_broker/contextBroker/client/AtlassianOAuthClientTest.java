package com.harsh.context_broker.contextBroker.client;

import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianAccessibleResourceResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianOAuthTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtlassianOAuthClientTest {

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void shouldExchangeCodeSuccessfully() throws Exception {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        properties.setClientId("cid");
        properties.setClientSecret("secret");
        properties.setRedirectUri("https://app/callback");

        RestClient mockAuth = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        RestClient mockApi = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        AtlassianOAuthTokenResponse response = new AtlassianOAuthTokenResponse("access-token", 3600L, "scope", "refresh");

        // stub deep-stub chain
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

        // verify chain invoked
        verify(mockAuth.post()).uri(eq("/oauth/token"));
    }

    @Test
    void shouldRejectMissingCode() {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        AtlassianOAuthClient client = new AtlassianOAuthClient(properties);

        assertThatThrownBy(() -> client.exchangeCode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authorization code is missing");
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
    void shouldGetAccessibleResourcesSuccessfully() throws Exception {
        JiraOAuthProperties properties = new JiraOAuthProperties();
        RestClient mockApi = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        RestClient mockAuth = mock(RestClient.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        List<AtlassianAccessibleResourceResponse> list = List.of(
                new AtlassianAccessibleResourceResponse("id", "name", "baseUrl", List.of("scopes"), "avatar")
        );

        when(mockApi.get()
                .uri(eq("/oauth/token/accessible-resources"))
                .header(eq("Authorization"), eq("Bearer token123"))
                .header(eq("Accept"), eq(org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                .retrieve()
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

        when(mockApi.get()
                .uri(eq("/oauth/token/accessible-resources"))
                .header(eq("Authorization"), eq("Bearer token123"))
                .header(eq("Accept"), eq(org.springframework.http.MediaType.APPLICATION_JSON_VALUE))
                .retrieve()
                .body(any(ParameterizedTypeReference.class)))
                .thenReturn(null);

        AtlassianOAuthClient client = new AtlassianOAuthClient(properties, mockAuth, mockApi);

        assertThatThrownBy(() -> client.getAccessibleResources("token123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Atlassian accessible-resources returned an empty response");
    }

}
