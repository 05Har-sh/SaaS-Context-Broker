package com.harsh.context_broker.contextBroker.dto.oauth.jira;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AtlassianOAuthTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        String scope,

        @JsonProperty("refresh_token")
        String refreshToken
) {
}
