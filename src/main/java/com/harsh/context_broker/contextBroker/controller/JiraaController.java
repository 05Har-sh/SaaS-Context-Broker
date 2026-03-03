package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.dto.JiraIncomingRequest;
import com.harsh.context_broker.contextBroker.dto.UpdateResponse;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/incoming")
public class JiraaController {
    private final IncidentService incidentService;

    public JiraaController(IncidentService incidentService){
        this.incidentService = incidentService;
    }
    @PostMapping("/jira")
    public UpdateResponse receiveJira(
            @Valid @RequestBody JiraIncomingRequest request
    ) {

        AlertResponse alert = incidentService.handleJiraUpdate(
                request.getIncidentKey(),
                request.getStatus()
        );

        UpdateResponse response = new UpdateResponse();
        response.setIncidentKey(request.getIncidentKey());
        response.setAlert(alert);

        return response;
    }
}
