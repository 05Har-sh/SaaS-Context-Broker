package com.harsh.context_broker.contextBroker.dto;


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

    private String timestamp;

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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public TimelineEventResponse(String source, String content,String eventType,String description, String timestamp) {
        this.source = source;
        this.content = content;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.description = description;
    }
    public TimelineEventResponse(){

    }
}
