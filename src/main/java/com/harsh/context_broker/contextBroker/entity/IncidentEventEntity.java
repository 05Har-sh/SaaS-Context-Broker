package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class IncidentEventEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String incidentKey;
    private String source;
    private String content;
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public IncidentEventEntity(Long id, String incidentKey, String source, String content, LocalDateTime timeStamp) {
        this.id = id;
        this.incidentKey = incidentKey;
        this.source = source;
        this.content = content;
        this.timestamp = timeStamp;
    }
    public IncidentEventEntity(){

    }
}
