package com.harsh.context_broker.contextBroker.dto;
import jakarta.validation.constraints.NotBlank;

public class JiraIncomingRequest {
    @NotBlank(message = "Incident key must not be blank")
    private String incidentKey;

    @NotBlank(message = "Status must not be blank ")
    private String status;

    public JiraIncomingRequest(String incidentKey, String status) {
        this.incidentKey = incidentKey;
        this.status = status;
    }

    public @NotBlank(message = "Incident key must not be blank") String getIncidentKey() {

        return incidentKey;
    }

    public void setIncidentKey(@NotBlank(message = "Incident key must not be blank") String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public @NotBlank(message = "Status must not be blank ") String getStatus() {
        return status;
    }

    public void setStatus(@NotBlank(message = "Status must not be blank ") String status) {
        this.status = status;
    }

    public JiraIncomingRequest() {
    }

}
