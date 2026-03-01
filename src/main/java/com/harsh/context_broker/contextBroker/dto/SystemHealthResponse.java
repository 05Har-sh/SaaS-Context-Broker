package com.harsh.context_broker.contextBroker.dto;

import java.util.List;

public class SystemHealthResponse {

    private String status;     // HEALTHY / DEGRADED / CRITICAL
    private int score;         // 0 - 100 health score
    private List<String> reasons;

    public SystemHealthResponse() {
    }

    public SystemHealthResponse(String status, int score, List<String> reasons) {
        this.status = status;
        this.score = score;
        this.reasons = reasons;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}