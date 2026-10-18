package com.harsh.context_broker.contextBroker.dto.oauth.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record JiraWebhookRegistrationRequest(
        String url,
        List<WebhookDetails> webhooks
) {
    public record WebhookDetails(
            @JsonProperty("jqlFilter")
            @JsonInclude(JsonInclude.Include.NON_EMPTY)
            String jqlFilter,
            List<String> events
    ) {
    }
}