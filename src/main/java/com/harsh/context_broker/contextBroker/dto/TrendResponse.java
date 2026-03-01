package com.harsh.context_broker.contextBroker.dto;

public class TrendResponse {
    private String date;   // YYYY-MM-DD
    private int count;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
