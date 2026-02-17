package com.harsh.context_broker.contextBroker.dto;

public class UpdateResponse {
    private String incidentKey;
    private AlertResponse alert;

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public AlertResponse getAlert() {
        return alert;
    }

    public void setAlert(AlertResponse alert) {
        this.alert = alert;
    }
}
