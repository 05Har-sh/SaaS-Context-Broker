package com.harsh.context_broker.contextBroker.dto;

public class MetricsResponse {
    private long total;
    private long active;
    private long critical;
    private long stale;
    private long escalationsToday;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getActive() {
        return active;
    }

    public void setActive(long active) {
        this.active = active;
    }

    public long getCritical() {
        return critical;
    }

    public void setCritical(long critical) {
        this.critical = critical;
    }

    public long getStale() {
        return stale;
    }

    public void setStale(long stale) {
        this.stale = stale;
    }

    public long getEscalationsToday() {
        return escalationsToday;
    }

    public void setEscalationsToday(long escalationsToday) {
        this.escalationsToday = escalationsToday;
    }

    public MetricsResponse(long total, long active, long critical, long stale, long escalationsToday) {
        this.total = total;
        this.active = active;
        this.critical = critical;
        this.stale = stale;
        this.escalationsToday = escalationsToday;
    }
    public MetricsResponse(){

    }
}
