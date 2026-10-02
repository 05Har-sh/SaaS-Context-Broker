package com.harsh.context_broker.contextBroker.dto.oauth.jira;

import java.util.List;

public record JiraWebhookRegistrationRequest(
        String url,
        List<WebhookDetails> webhooks
) {
    public record WebhookDetails(
            String jqlFilter,
            List<String> events
    ) {
    }
}