package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.*;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import com.harsh.context_broker.contextBroker.service.TimelineService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/incident")
@CrossOrigin(origins = "http://localhost:3000")
public class IncidentController {
    private final IncidentService incidentService;
    private final TimelineService timelineService;

     public IncidentController(IncidentService incidentService, TimelineService timelineService){
         this.incidentService = incidentService;
         this.timelineService = timelineService;
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
         return timelineService.getTimelineForIncident(incidentKey);
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

    @GetMapping("/highest-risk")
    public HighestRiskResponse getHighestRisk() {
        return incidentService.getHighestRisk();
    }

    @GetMapping("/severity-distribution")
    public List<SeverityDistributionResponse> getSeverityDistribution() {
        return incidentService.getSeverityDistribution();
    }

    @GetMapping("/trend")
    public List<TrendResponse> getTrend() {
        return incidentService.getTrend();
    }

//    @GetMapping("/search")
//    public List<IncidentResponse> search(@RequestParam String q) {
//        return incidentService.searchIncidents(q);
//    }
}
