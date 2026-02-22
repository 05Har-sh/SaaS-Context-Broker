package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.*;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/incident")
@CrossOrigin(origins = "http://localhost:3000")
public class IncidentController {
    private final IncidentService incidentService;

     public IncidentController(IncidentService incidentService){
         this.incidentService = incidentService;
     }
    @GetMapping("/{incidentKey}")
     public IncidentResponse getIncidentDetails(@PathVariable String incidentKey){
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

         @GetMapping("/incident-details/{incidentKey}")
     public IncidentDetailsResponse getIncidentDetailsDetails(@PathVariable String incidentKey){
         return incidentService.getIncidentDetails(incidentKey);
     }

     @GetMapping("/metrics")
     public MetricsResponse getMetrics() {
         return incidentService.getMetrics();
     }

    @GetMapping("/all")
    public List<IncidentResponse> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    @GetMapping("/system-health")
    public SystemHealthResponse systemHealth() {
        return incidentService.getSystemHealth();
    }

    @GetMapping("/risk-score/{incidentKey}")
    public RiskScoreResponse riskScore(@PathVariable String incidentKey) {
        return incidentService.getRiskScore(incidentKey);
    }

    @GetMapping("/priority/{incidentKey}")
    public PriorityResponse priority(@PathVariable String incidentKey) {
        return incidentService.getPriority(incidentKey);
    }

//    @GetMapping("/search")
//    public List<IncidentResponse> search(@RequestParam String q) {
//        return incidentService.searchIncidents(q);
//    }
}
