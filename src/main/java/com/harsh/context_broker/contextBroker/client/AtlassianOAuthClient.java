package com.harsh.context_broker.contextBroker.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianAccessibleResourceResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.AtlassianOAuthTokenResponse;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationRequest;
import com.harsh.context_broker.contextBroker.dto.oauth.jira.JiraWebhookRegistrationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class AtlassianOAuthClient {
    private static final Logger log = LoggerFactory.getLogger(AtlassianOAuthClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
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
        log.info("Calling Atlassian OAuth token endpoint: clientIdConfigured={}, redirectUri={}, codeLength={}",
                properties.getClientId() != null && !properties.getClientId().isBlank(),
                properties.getRedirectUri(),
                code.length());
        try {
            AtlassianOAuthTokenResponse response =
                    authRestClient.post()
                            .uri("/oauth/token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(AtlassianOAuthTokenResponse.class);

            if (response == null) {
                log.error("Atlassian OAuth token endpoint returned an empty response for redirectUri={}", properties.getRedirectUri());
                throw new IllegalStateException("Atlassian OAuth returned an empty response");
            }
            log.info("Atlassian OAuth token endpoint responded: expiresIn={}, scopePresent={}, refreshTokenPresent={}",
                    response.expiresIn(),
                    response.scope() != null && !response.scope().isBlank(),
                    response.refreshToken() != null && !response.refreshToken().isBlank());
            return response;
        } catch (Exception e) {
            log.error("Atlassian OAuth token exchange failed: clientIdConfigured={}, redirectUri={}, codeLength={}",
                    properties.getClientId() != null && !properties.getClientId().isBlank(),
                    properties.getRedirectUri(),
                    code.length(),
                    e);
            throw e;
        }
    }
    public List<AtlassianAccessibleResourceResponse> getAccessibleResources(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is missing");
        }

        log.info("Calling Atlassian accessible-resources endpoint with accessTokenLength={}", accessToken.length());
        try {
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
                log.error("Atlassian accessible-resources endpoint returned an empty response.");
                throw new IllegalStateException("Atlassian accessible-resources returned an empty response");
            }
            log.info("Atlassian accessible-resources response count={}", response.size());
            return response;
        } catch (Exception e) {
            log.error("Fetching Jira accessible resources failed for accessTokenLength={}", accessToken.length(), e);
            throw e;
        }
    }
    public JiraWebhookRegistrationResponse registerWebhooks(
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

        log.info("Calling Atlassian webhook registration API: cloudId={}, webhookUrl={}, eventCount={}",
                cloudId,
                request.url(),
                request.webhooks() != null ? request.webhooks().size() : 0);
        try {
            log.info("Jira webhook request body: {}",
                    objectMapper.writeValueAsString(request));
            String rawResponseBody = apiRestClient.post()
                    .uri("/ex/jira/{cloudId}/rest/api/3/webhook", cloudId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((requestSpec, response) -> {
                        String body = readResponseBody(response);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            log.info("Jira webhook registration HTTP {} for cloudId={} and body={}",
                                    response.getStatusCode().value(),
                                    cloudId,
                                    body);
                            return body;
                        }

                        log.error("Jira webhook registration failed with HTTP {} for cloudId={} and body={}",
                                response.getStatusCode().value(),
                                cloudId,
                                body);
                        throw new IllegalStateException("Jira webhook registration failed with HTTP "
                                + response.getStatusCode().value() + ": " + body);
                    });

            if (rawResponseBody == null || rawResponseBody.isBlank()) {
                log.error("Jira webhook registration endpoint returned an empty response for cloudId={}", cloudId);
                throw new IllegalStateException("Jira webhook registration returned an empty response");
            }

            JiraWebhookRegistrationResponse response =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(rawResponseBody, JiraWebhookRegistrationResponse.class);

            if (response == null || response.webhookRegistrationResult() == null
                    || response.webhookRegistrationResult().isEmpty()) {
                log.error("Jira webhook registration response body was missing webhookRegistrationResult data for cloudId={}. Raw body={}",
                        cloudId,
                        rawResponseBody);
                throw new IllegalStateException("Jira webhook registration response is missing webhookRegistrationResult");
            }

            log.info("Jira webhook registration response count={} for cloudId={}", response.webhookRegistrationResult().size(), cloudId);
            return response;
        } catch (IOException e) {
            log.error("Jira webhook registration returned malformed JSON for cloudId={}, webhookUrl={}", cloudId, request.url(), e);
            throw new IllegalStateException("Jira webhook registration returned malformed JSON", e);
        } catch (Exception e) {
            log.error("Jira webhook registration failed for cloudId={}, webhookUrl={}", cloudId, request.url(), e);
            throw e;
        }
    }

    public void deleteWebhook(String cloudId, String webhookId, String accessToken) {
        if (cloudId == null || cloudId.isBlank()) {
            throw new IllegalArgumentException("Cloud ID is missing for Jira webhook cleanup");
        }
        if (webhookId == null || webhookId.isBlank()) {
            throw new IllegalArgumentException("Webhook ID is missing for Jira webhook cleanup");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is missing for Jira webhook cleanup");
        }

        apiRestClient.delete()
                .uri("/ex/jira/{cloudId}/rest/api/3/webhook/{webhookId}", cloudId, webhookId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();
    }

    private String readResponseBody(ClientHttpResponse response) {
        try {
            if (response.getBody() == null) {
                return "";
            }
            return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Unable to read Jira webhook registration response body", e);
            return "";
        }
    }
}
