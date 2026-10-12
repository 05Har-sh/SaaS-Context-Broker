package com.harsh.context_broker.contextBroker.dto.integration;

/**
 * Safe summary returned to the frontend for listing integrations.
 * Contains only non-sensitive fields.
 */
public class IntegrationSummary {
    private Long id;
    private String type; // e.g., "SLACK" or "JIRA"
    private boolean connected;
    private String displayName;

    public IntegrationSummary() {
    }

    public IntegrationSummary(Long id, String type, boolean connected, String displayName) {
        this.id = id;
        this.type = type;
        this.connected = connected;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}