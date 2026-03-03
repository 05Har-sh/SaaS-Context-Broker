package com.harsh.context_broker.contextBroker.dto;

public class ApiSuccessResponse {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ApiSuccessResponse(String message) {
        this.message = message;
    }

    public ApiSuccessResponse() {
    }
}
