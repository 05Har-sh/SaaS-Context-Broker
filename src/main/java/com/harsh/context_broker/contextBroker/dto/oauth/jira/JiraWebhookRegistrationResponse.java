package com.harsh.context_broker.contextBroker.dto.oauth.jira;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraWebhookRegistrationResponse(
        @JsonProperty("webhookRegistrationResult")
        List<WebhookResult> webhookRegistrationResult
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookResult(
            Long createdWebhookId,
            List<String> errors
    ) {}
}