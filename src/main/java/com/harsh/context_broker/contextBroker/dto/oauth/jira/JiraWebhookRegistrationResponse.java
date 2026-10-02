package com.harsh.context_broker.contextBroker.dto.oauth.jira;
import java.util.List;

public record JiraWebhookRegistrationResponse(
        Long createdWebhookId,
        List<String> errors
) {
}