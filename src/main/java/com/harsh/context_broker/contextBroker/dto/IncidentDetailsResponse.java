package com.harsh.context_broker.contextBroker.dto;

public class IncidentDetailsResponse {
    private String incidentKey;
    private String lastMsg;
    private String jiraStatus;
    private String severity;
    private String lastUpdated;
    private String postedAt;

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

    public String getJiraStatus() {
        return jiraStatus;
    }

    public void setJiraStatus(String jiraStatus) {
        this.jiraStatus = jiraStatus;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(String postedAt) {
        this.postedAt = postedAt;
    }

    public IncidentDetailsResponse(String incidentKey, String lastMsg, String jiraStatus, String severity, String lastUpdated, String postedAt) {
        this.incidentKey = incidentKey;
        this.lastMsg = lastMsg;
        this.jiraStatus = jiraStatus;
        this.severity = severity;
        this.lastUpdated = lastUpdated;
        this.postedAt = postedAt;
    }
    public IncidentDetailsResponse(){

    }
}
