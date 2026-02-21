package com.harsh.context_broker.contextBroker.dto;

import java.time.LocalDateTime;

public class TimelineEventResponse {

    private String source;
    private String content;
    private LocalDateTime timeStamp;

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

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public TimelineEventResponse(String source, String content, LocalDateTime timeStamp) {
        this.source = source;
        this.content = content;
        this.timeStamp = timeStamp;
    }
    public TimelineEventResponse(){

    }
}
