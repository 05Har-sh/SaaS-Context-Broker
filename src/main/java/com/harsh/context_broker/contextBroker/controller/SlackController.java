package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.dto.SlackIncomingRequest;
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
public class SlackController {
    private final IncidentService incidentService;

    public SlackController(IncidentService incidentService){
        this.incidentService = incidentService;
    }
    @PostMapping("/slack")
    public UpdateResponse recieveSlack(@Valid @RequestBody SlackIncomingRequest request){

        AlertResponse alert = incidentService.handleUpdate(request.getIncidentKey(), request.getMessage());

        UpdateResponse response = new UpdateResponse();
        response.setIncidentKey(request.getIncidentKey());
        response.setAlert(alert);

        return response;
    }
}
