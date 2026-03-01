package com.harsh.context_broker.contextBroker.dto;

public class HighestRiskResponse {
    private String incidentKey;
    private int riskScore;

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }
}
