package com.harsh.context_broker.contextBroker.dto;

public class RiskScoreResponse {
    private String incidentKey;
    private int riskScore;
    private String reason;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RiskScoreResponse(String incidentKey, int riskScore, String reason) {
        this.incidentKey = incidentKey;
        this.riskScore = riskScore;
        this.reason = reason;
    }
    public RiskScoreResponse(){

    }
}
