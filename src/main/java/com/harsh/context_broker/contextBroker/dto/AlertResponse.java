package com.harsh.context_broker.contextBroker.dto;

import com.harsh.context_broker.contextBroker.model.Severity;

public class AlertResponse {
    private Severity severity;
    private int score;
    private String reason;

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
