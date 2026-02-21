package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.IncidentResponse;
import com.harsh.context_broker.contextBroker.dto.TimelineEventResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/incident")
public class IncidentController {
    private final IncidentService incidentService;

     public IncidentController(IncidentService incidentService){
         this.incidentService = incidentService;
     }
    @GetMapping("/{incidentKey}")
     public IncidentResponse getIncident(@PathVariable String incidentKey){
         IncidentEntity incident = incidentService.getIncidentByKey(incidentKey);

         IncidentResponse response = new IncidentResponse();
         response.setIncidentKey(incident.getIncidentKey());
         response.setLastMsg(incident.getLastMsg());
         response.setLastUpdated(incident.getLastUpdated().toString());

         boolean isStale = Duration
                 .between(incident.getLastUpdated(), LocalDateTime.now())
                 .toMinutes() > 1;
         response.setStale(isStale);

         return response ;
     }

     @GetMapping("/{incidentKey}/timeline")
     public List<TimelineEventResponse> getTimeLine(@PathVariable String incidentKey){
         return incidentService.getTimeLine(incidentKey);
     }
}
