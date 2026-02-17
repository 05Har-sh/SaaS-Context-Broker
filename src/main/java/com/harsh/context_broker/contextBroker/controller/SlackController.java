package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.dto.UpdateResponse;
import com.harsh.context_broker.contextBroker.service.IncidentService;
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
    public UpdateResponse recieveSlack(@RequestBody Map<String, Object> payload){
        String incidentKey = payload.get("incidentKey").toString();
        String message = payload.get("message").toString();
        AlertResponse alert = incidentService.handleUpdate(incidentKey, message);

        UpdateResponse response = new UpdateResponse();
        response.setIncidentKey(incidentKey);
        response.setAlert(alert);

        return response;
    }
}
