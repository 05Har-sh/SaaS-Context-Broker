package com.harsh.context_broker.contextBroker.dto;

public class PriorityResponse {
    private String incidentKey;
    private String priority;   // LOW / MEDIUM / HIGH / P1

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public PriorityResponse(String incidentKey, String priority) {
        this.incidentKey = incidentKey;
        this.priority = priority;
    }
    public PriorityResponse(){

    }
}
