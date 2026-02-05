package com.harsh.context_broker.contextBroker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

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
}
