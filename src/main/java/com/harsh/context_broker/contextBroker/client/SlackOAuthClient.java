package com.harsh.context_broker.contextBroker.client;

import com.harsh.context_broker.contextBroker.config.slackoauth.SlackOAuthProperties;
import com.harsh.context_broker.contextBroker.dto.oauth.SlackOAuthAccessResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class SlackOAuthClient {

    private final RestClient restClient;
    private final SlackOAuthProperties properties;

    public SlackOAuthClient(SlackOAuthProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://slack.com")
                .build();

        this.properties = properties;
    }
    public SlackOAuthAccessResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add(code, "code");
        form.add("redirect_uri", properties.getRedirectUri());

        SlackOAuthAccessResponse response = restClient.post()
                .uri("/api/oauth.v2.access")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(SlackOAuthAccessResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Slack OAuth returned an empty response"
            );
        }
        return response;
    }
}
