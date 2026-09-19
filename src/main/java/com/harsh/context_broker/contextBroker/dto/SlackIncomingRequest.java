package com.harsh.context_broker.contextBroker.dto;

import jakarta.validation.constraints.NotBlank;

public class SlackIncomingRequest {
    @NotBlank(message = "Incident key must not be blank")
    private String incidentKey;

    @NotBlank(message = "message must not be blank ")
    private String message;

    public @NotBlank(message = "Incident key must not be blank") String getIncidentKey() {
        return incidentKey;
    }

    @NotBlank(message = "Team ID must not be blank")
    private String teamId;

    public void setIncidentKey(@NotBlank(message = "Incident key must not be blank") String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public @NotBlank(message = "message must not be blank ") String getMessage() {
        return message;
    }

    public void setMessage(@NotBlank(message = "message must not be blank ") String message) {
        this.message = message;
    }

    public SlackIncomingRequest(
            String teamId,
            String incidentKey,
            String message) {

        this.teamId = teamId;
        this.incidentKey = incidentKey;
        this.message = message;
    }

    public SlackIncomingRequest() {
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}
