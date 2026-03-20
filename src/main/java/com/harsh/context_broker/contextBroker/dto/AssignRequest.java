package com.harsh.context_broker.contextBroker.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRequest {
    @NotBlank(message = "who is the incident assigned to?")
    private String assignedTo;

    public @NotBlank(message = "who is the incident assigned to?") String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(@NotBlank(message = "who is the incident assigned to?") String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public AssignRequest(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public AssignRequest() {
    }
}
