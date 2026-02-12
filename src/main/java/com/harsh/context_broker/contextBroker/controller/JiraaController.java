package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.UpdateResponse;
import com.harsh.context_broker.contextBroker.service.IncidentService;
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
    public UpdateResponse recieveJira(@RequestBody Map<String, Object> payload){
        String incidentKey = payload.get("incidentKey").toString();
        String status = payload.get("status").toString();

        String alert = incidentService.handleUpdate(incidentKey, "Jira status :" + status);

        UpdateResponse response = new UpdateResponse();
        response.setIncidentKey(incidentKey);
        response.setAlert(alert);

        return response;
    }

}
