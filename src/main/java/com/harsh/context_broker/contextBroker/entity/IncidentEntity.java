package com.harsh.context_broker.contextBroker.entity;

import com.harsh.context_broker.contextBroker.model.Severity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class IncidentEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String incidentKey;
    private String LastMsg;
    private LocalDateTime lastUpdated;
    private LocalDateTime postedAt;
    private String jiraStatus;
    private LocalDateTime lastEscalatedAt;
    private LocalDateTime lastAlertedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    public LocalDateTime getLastAlertedAt() {
        return lastAlertedAt;
    }

    public void setLastAlertedAt(LocalDateTime lastAlertedAt) {
        this.lastAlertedAt = lastAlertedAt;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public LocalDateTime getLastEscalatedAt() {
        return lastEscalatedAt;
    }

    public void setLastEscalatedAt(LocalDateTime lastEscalatedAt) {
        this.lastEscalatedAt = lastEscalatedAt;
    }

    public String getJiraStatus() {
        return jiraStatus;
    }

    public void setJiraStatus(String jiraStatus) {
        this.jiraStatus = jiraStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIncidentKey() {
        return incidentKey;
    }

    public void setIncidentKey(String incidentKey) {
        this.incidentKey = incidentKey;
    }

    public String getLastMsg() {
        return LastMsg;
    }

    public void setLastMsg(String lastMsg) {
        LastMsg = lastMsg;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public IncidentEntity(Long id, String incidentKey, String lastMsg, LocalDateTime lastUpdated, LocalDateTime postedAt, String jiraStatus, Severity severity, LocalDateTime lastAlertedAt) {
        this.id = id;
        this.incidentKey = incidentKey;
        this.LastMsg = lastMsg;
        this.lastUpdated = lastUpdated;
        this.postedAt = postedAt;
        this.jiraStatus = jiraStatus;
        this.severity = severity;
        this.lastAlertedAt = lastAlertedAt;
    }
    public IncidentEntity(){

    }
}
