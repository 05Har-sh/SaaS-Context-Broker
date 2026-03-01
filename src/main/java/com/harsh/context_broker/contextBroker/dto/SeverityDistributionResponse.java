package com.harsh.context_broker.contextBroker.dto;

public class SeverityDistributionResponse {
    private String severity;
    private int count;

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
