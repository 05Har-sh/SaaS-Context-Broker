package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.service.IncidentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/incoming")
public class SlackController {
    private final IncidentService incidentService;

    public SlackController(IncidentService incidentService){
        this.incidentService = incidentService;
    }
    @PostMapping("/slack")
    public void recieveSlack(@RequestBody Map<String, Object> payload){
        String incidentKey = payload.get("incidentKey").toString();
        String message = payload.get("message").toString();

        incidentService.handleUpdate(incidentKey, message);
    }
}
