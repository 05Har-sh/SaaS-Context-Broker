package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class TimelineEventEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String incidentKey;

    private String eventType;   // CREATED / UPDATED / ALERT_SENT

    private String description;

    private LocalDateTime timestamp;

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TimelineEventEntity(Long id, String incidentKey, String eventType, String description, LocalDateTime timestamp) {
        this.id = id;
        this.incidentKey = incidentKey;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
    }

    public TimelineEventEntity() {
    }
}
