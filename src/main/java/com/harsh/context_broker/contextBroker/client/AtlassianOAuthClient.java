package com.harsh.context_broker.contextBroker.client;

import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianAccessibleResourceResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianOAuthTokenResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationRequest;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AtlassianOAuthClient {
    private final RestClient authRestClient;
    private final RestClient apiRestClient;
    private final JiraOAuthProperties properties;

    @Autowired
    public AtlassianOAuthClient(JiraOAuthProperties properties) {
        this.apiRestClient = RestClient.builder()
                .baseUrl("https://api.atlassian.com")
                .build();

        this.authRestClient = RestClient.builder()
                .baseUrl("https://auth.atlassian.com")
                .build();

        this.properties = properties;
    }

    // Package-private constructor to allow injecting mocked RestClient instances in tests
    AtlassianOAuthClient(JiraOAuthProperties properties, RestClient authRestClient, RestClient apiRestClient) {
        this.apiRestClient = apiRestClient;
        this.authRestClient = authRestClient;
        this.properties = properties;
    }
    public AtlassianOAuthTokenResponse exchangeCode(String code) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code is missing");
        }

        Map<String, String> requestBody = Map.of(
                "grant_type", "authorization_code",
                "client_id", properties.getClientId(),
                "client_secret", properties.getClientSecret(),
                "code", code,
                "redirect_uri", properties.getRedirectUri()
        );
        AtlassianOAuthTokenResponse response =
                authRestClient.post()
                        .uri("/oauth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(AtlassianOAuthTokenResponse.class);

        if (response == null) {
            throw new IllegalStateException("Atlassian OAuth returned an empty response");
        }
        return response;
    }
    public List<AtlassianAccessibleResourceResponse> getAccessibleResources(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is missing");
        }

        List<AtlassianAccessibleResourceResponse> response =
                apiRestClient.get()
                        .uri("/oauth/token/accessible-resources")
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .header(
                                "Accept",
                                MediaType.APPLICATION_JSON_VALUE
                        )
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<List<AtlassianAccessibleResourceResponse>>() {});

        if (response == null) {
            throw new IllegalStateException("Atlassian accessible-resources returned an empty response");
        }
        return response;
    }
    public List<JiraWebhookRegistrationResponse> registerWebhooks(
            String cloudId,
            String accessToken,
            JiraWebhookRegistrationRequest request
    ) {
        if (cloudId == null || cloudId.isBlank()) {
            throw new IllegalArgumentException("Cloud ID is missing");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is missing");
        }

        if (request == null) {
            throw new IllegalArgumentException("Webhook request is missing");
        }

        List<JiraWebhookRegistrationResponse> response =
                apiRestClient.post()
                        .uri("/ex/jira/{cloudId}/rest/api/3/webhook", cloudId)
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        List<JiraWebhookRegistrationResponse>
                                        >() {}
                        );

        if (response == null) {
            throw new IllegalStateException(
                    "Jira webhook registration returned an empty response"
            );
        }

        return response;
    }
}
