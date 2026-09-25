package com.harsh.context_broker.contextBroker.dto.integration;

/**
 * {
 *   "teamId": "T123456",
 *   "signingSecret": "actual-slack-signing-secret"
 * }
 */
public class SlackIntegrationRequest {
    private String teamId;
    private String signingSecret;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }
}
