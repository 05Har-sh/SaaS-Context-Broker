package com.harsh.context_broker.contextBroker.dto;

import com.harsh.context_broker.contextBroker.model.Severity;

public class IncidentResponse {
    private String incidentKey;
    private String lastMsg;
    private String lastUpdated;
    private boolean stale;
    private String jiraStatus;
    private String incidentStatus;
    private int riskScore;
    private Severity severity;
    private String assignedTo;

    // ── Getters & Setters ──

    public String getIncidentKey() { return incidentKey; }
    public void setIncidentKey(String incidentKey) { this.incidentKey = incidentKey; }

    public String getLastMsg() { return lastMsg; }
    public void setLastMsg(String lastMsg) { this.lastMsg = lastMsg; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isStale() { return stale; }
    public void setStale(boolean stale) { this.stale = stale; }

    public String getJiraStatus() { return jiraStatus; }
    public void setJiraStatus(String jiraStatus) { this.jiraStatus = jiraStatus; }

    public String getIncidentStatus() { return incidentStatus; }
    public void setIncidentStatus(String incidentStatus) { this.incidentStatus = incidentStatus; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public IncidentResponse(String incidentKey, String lastMsg, String lastUpdated, boolean stale,
                            String jiraStatus, String incidentStatus, int riskScore,
                            Severity severity, String assignedTo) {
        this.incidentKey = incidentKey;
        this.lastMsg = lastMsg;
        this.lastUpdated = lastUpdated;
        this.stale = stale;
        this.jiraStatus = jiraStatus;
        this.incidentStatus = incidentStatus;
        this.riskScore = riskScore;
        this.severity = severity;
        this.assignedTo = assignedTo;
    }

    public IncidentResponse() {}
}
