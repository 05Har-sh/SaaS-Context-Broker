package com.harsh.context_broker.contextBroker.dto;

import java.time.LocalDateTime;

public class TimelineEventResponse {

    private String source;
    private String content;
    private String eventType;
    private String description;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public TimelineEventResponse(String source, String content,String eventType,String description, LocalDateTime timeStamp) {
        this.source = source;
        this.content = content;
        this.timeStamp = timeStamp;
        this.eventType = eventType;
        this.description = description;
    }
    public TimelineEventResponse(){

    }
}
