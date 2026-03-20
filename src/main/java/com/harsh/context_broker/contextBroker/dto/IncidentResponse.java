package com.harsh.context_broker.contextBroker.dto;

import com.harsh.context_broker.contextBroker.model.Severity;
import jakarta.persistence.Column;

public class IncidentResponse {
    private String incidentKey;
    private String lastMsg;
    private String lastUpdated;
    private boolean stale;
    private String jiraStatus;
    private int riskScore;

    @Column(name = "severity")
    private Severity severity;
    private String assignedTo;

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getJiraStatus() {
        return jiraStatus;
    }

    public void setJiraStatus(String jiraStatus) {
        this.jiraStatus = jiraStatus;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public void setLastMsg(String lastMsg) {
        this.lastMsg = lastMsg;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public IncidentResponse(String incidentKey, String lastMsg, String lastUpdated, boolean stale, String jiraStatus, int riskScore, Severity severity, String assignedTo) {
        this.incidentKey = incidentKey;
        this.lastMsg = lastMsg;
        this.lastUpdated = lastUpdated;
        this.stale = stale;
        this.jiraStatus = jiraStatus;
        this.riskScore = riskScore;
        this.severity = severity;
        this.assignedTo = assignedTo;
    }

    public IncidentResponse() {
    }
}
