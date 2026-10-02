package com.harsh.context_broker.contextBroker.dto.oauth.jira;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AtlassianAccessibleResourceResponse(
        String id,
        String name,
        String url,
        List<String> scopes,

        @JsonProperty("avatarUrl")
        String avatarUrl
) {
}