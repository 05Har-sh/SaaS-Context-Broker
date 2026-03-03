package com.harsh.context_broker.contextBroker.dto;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class JiraIncomingRequest {
    @NotBlank(message = "Incident key must not be blank")
    private String incidentKey;

    @NotNull(message = "Status must not be null ")
    private JiraStatus status;

    public JiraIncomingRequest(String incidentKey, JiraStatus status) {
        this.incidentKey = incidentKey;
        this.status = status;
    }

    public @NotBlank(message = "Incident key must not be blank") String getIncidentKey() {

        return incidentKey;
    }

    public void setIncidentKey(@NotBlank(message = "Incident key must not be blank") String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public @NotBlank(message = "Status must not be blank ") JiraStatus getStatus() {
        return status;
    }

    public void setStatus(@NotBlank(message = "Status must not be blank ") JiraStatus status) {
        this.status = status;
    }

    public JiraIncomingRequest() {
    }

}
