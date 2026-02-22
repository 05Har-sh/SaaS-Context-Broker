package com.harsh.context_broker.contextBroker.dto;

public class SystemHealthResponse {
    private String status;      // HEALTHY / WARNING / CRITICAL
    private long totalIncidents;
    private long staleIncidents;
    private long criticalIncidents;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public long getStaleIncidents() {
        return staleIncidents;
    }

    public void setStaleIncidents(long staleIncidents) {
        this.staleIncidents = staleIncidents;
    }

    public long getCriticalIncidents() {
        return criticalIncidents;
    }

    public void setCriticalIncidents(long criticalIncidents) {
        this.criticalIncidents = criticalIncidents;
    }

    public SystemHealthResponse(String status, long totalIncidents, long staleIncidents, long criticalIncidents) {
        this.status = status;
        this.totalIncidents = totalIncidents;
        this.staleIncidents = staleIncidents;
        this.criticalIncidents = criticalIncidents;
    }
    public SystemHealthResponse(){

    }
}
