package com.harsh.context_broker.contextBroker.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SlackOAuthAccessResponse(
        boolean ok,

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        String scope,

        @JsonProperty("bot_user_id")
        String botUserId,

        @JsonProperty("app_id")
        String appId,

        Team team,

        String error
) {
    public record Team(
            String id,
            String name
    ) {
    }
}
