package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.LocalDateTime;

@Entity
public class TimelineEventEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String incidentKey;
    private String eventType;       // CREATED / SEVERITY_EVALUATED / ALERT_SENT / JIRA_UPDATE etc.
    private String source;          // SLACK / JIRA / SYSTEM
    private String description;
    @Lob
    private String content;         // Raw event content (nullable)
    private LocalDateTime timestamp;

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIncidentKey() { return incidentKey; }
    public void setIncidentKey(String incidentKey) { this.incidentKey = incidentKey; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // ── Constructors ──

    public TimelineEventEntity(Long id, String incidentKey, String eventType, String source,
                               String description, String content, LocalDateTime timestamp) {
        this.id = id;
        this.incidentKey = incidentKey;
        this.eventType = eventType;
        this.source = source;
        this.description = description;
        this.content = content;
        this.timestamp = timestamp;
    }

    public TimelineEventEntity() {}
}
