package com.harsh.context_broker.contextBroker.entity;

import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class IncidentEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String incidentKey;
    private String lastMsg;
    private LocalDateTime lastUpdated;
    private LocalDateTime postedAt;

    @Enumerated(EnumType.STRING)
    private JiraStatus jiraStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_status")
    private IncidentStatus incidentStatus;

    private LocalDateTime lastEscalatedAt;
    private LocalDateTime lastAlertedAt;
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private Severity severity;

    private String assignedTo;

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentKey() { return incidentKey; }
    public void setIncidentKey(String incidentKey) { this.incidentKey = incidentKey; }

    public String getLastMsg() { return lastMsg; }
    public void setLastMsg(String lastMsg) { this.lastMsg = lastMsg; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }

    public JiraStatus getJiraStatus() { return jiraStatus; }
    public void setJiraStatus(JiraStatus jiraStatus) { this.jiraStatus = jiraStatus; }

    public IncidentStatus getIncidentStatus() { return incidentStatus; }
    public void setIncidentStatus(IncidentStatus incidentStatus) { this.incidentStatus = incidentStatus; }

    public LocalDateTime getLastEscalatedAt() { return lastEscalatedAt; }
    public void setLastEscalatedAt(LocalDateTime lastEscalatedAt) { this.lastEscalatedAt = lastEscalatedAt; }

    public LocalDateTime getLastAlertedAt() { return lastAlertedAt; }
    public void setLastAlertedAt(LocalDateTime lastAlertedAt) { this.lastAlertedAt = lastAlertedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    // ── Constructors ──

    public IncidentEntity(Long id, String incidentKey, JiraStatus jiraStatus, IncidentStatus incidentStatus,
                          String lastMsg, LocalDateTime lastUpdated, LocalDateTime postedAt,
                          Severity severity, LocalDateTime lastAlertedAt, LocalDateTime resolvedAt, String assignedTo) {
        this.id = id;
        this.incidentKey = incidentKey;
        this.lastMsg = lastMsg;
        this.lastUpdated = lastUpdated;
        this.postedAt = postedAt;
        this.severity = severity;
        this.jiraStatus = jiraStatus;
        this.incidentStatus = incidentStatus;
        this.lastAlertedAt = lastAlertedAt;
        this.resolvedAt = resolvedAt;
        this.assignedTo = assignedTo;
    }

    public IncidentEntity() {}
}