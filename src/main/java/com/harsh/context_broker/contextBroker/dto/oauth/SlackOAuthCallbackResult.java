package com.harsh.context_broker.contextBroker.dto.oauth;

public record SlackOAuthCallbackResult(
        String tenantId,
        String userId,
        SlackOAuthAccessResponse slackResponse
) {
}
