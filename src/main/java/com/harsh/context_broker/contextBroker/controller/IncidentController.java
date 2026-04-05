package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.*;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import com.harsh.context_broker.contextBroker.service.TimelineService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incident")
@CrossOrigin(origins = "http://localhost:3000")
public class IncidentController {
    private final IncidentService incidentService;
    private final TimelineService timelineService;

    public IncidentController(IncidentService incidentService, TimelineService timelineService) {
        this.incidentService = incidentService;
        this.timelineService = timelineService;
    }

    /**
     * returns complete IncidentResponse including severity, riskScore, status.
     */
    @GetMapping("/{incidentKey}")
    public IncidentResponse getIncident(@PathVariable String incidentKey) {
        return incidentService.getIncidentResponse(incidentKey);
    }

    @GetMapping("/{incidentKey}/timeline")
    public List<TimelineEventResponse> getTimeLine(@PathVariable String incidentKey) {
        return timelineService.getTimelineForIncident(incidentKey);
    }

    @GetMapping("/incident-details/{incidentKey}")
    public IncidentDetailsResponse getIncidentDetails(@PathVariable String incidentKey) {
        return incidentService.getIncidentDetails(incidentKey);
    }

    @GetMapping("/metrics")
    public MetricsResponse getMetrics() {
        return incidentService.getMetrics();
    }

    @GetMapping("/all")
    public Page<IncidentResponse> getAllIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) Boolean stale,
            @RequestParam(required = false) JiraStatus jiraStatus
    ) {
        return incidentService.getAllIncidents(page, size, severity, assignedTo, stale, jiraStatus);
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

    @PostMapping("/{incidentKey}/assign")
    public ApiSuccessResponse assignIncident(
            @PathVariable String incidentKey,
            @Valid @RequestBody AssignRequest request) {
        incidentService.assignIncident(incidentKey, request.getAssignedTo());
        return new ApiSuccessResponse("Incident assigned successfully");
    }
}
