package com.harsh.context_broker.contextBroker.dto;

public class UpdateResponse {
    private String incidentKey;
    private String alert;

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public UpdateResponse(String incidentKey, String alert) {
        this.incidentKey = incidentKey;
        this.alert = alert;
    }
    public UpdateResponse(){

    }
}
