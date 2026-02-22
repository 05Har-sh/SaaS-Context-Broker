package com.harsh.context_broker.contextBroker.dto;

public class IncidentTableResponse {
    private String incidentKey;
    private String severity;
    private String jiraStatus;
    private String lastUpdated;
    private boolean stale;

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getJiraStatus() {
        return jiraStatus;
    }

    public void setJiraStatus(String jiraStatus) {
        this.jiraStatus = jiraStatus;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public IncidentTableResponse(String incidentKey, String severity, String jiraStatus, String lastUpdated, boolean stale) {
        this.incidentKey = incidentKey;
        this.severity = severity;
        this.jiraStatus = jiraStatus;
        this.lastUpdated = lastUpdated;
        this.stale = stale;
    }
    public IncidentTableResponse(){

    }
}
