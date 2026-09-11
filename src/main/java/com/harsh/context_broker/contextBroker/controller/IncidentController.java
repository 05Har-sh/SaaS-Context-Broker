package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.*;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import com.harsh.context_broker.contextBroker.service.TimelineService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public IncidentResponse getIncident(@PathVariable String incidentKey) {
        return incidentService.getIncidentResponse(incidentKey);
    }

    @GetMapping("/{incidentKey}/timeline")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<TimelineEventResponse> getTimeLine(@PathVariable String incidentKey) {
        return timelineService.getTimelineForIncident(incidentKey);
    }

    @GetMapping("/incident-details/{incidentKey}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public IncidentDetailsResponse getIncidentDetails(@PathVariable String incidentKey) {
        return incidentService.getIncidentDetails(incidentKey);
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public MetricsResponse getMetrics() {
        return incidentService.getMetrics();
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
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
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public SystemHealthResponse systemHealth() {
        return incidentService.getSystemHealth();
    }

    @GetMapping("/risk-score/{incidentKey}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public RiskScoreResponse riskScore(@PathVariable String incidentKey) {
        return incidentService.getRiskScore(incidentKey);
    }

    @GetMapping("/priority/{incidentKey}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public PriorityResponse priority(@PathVariable String incidentKey) {
        return incidentService.getPriority(incidentKey);
    }

    @GetMapping("/highest-risk")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public HighestRiskResponse getHighestRisk() {
        return incidentService.getHighestRisk();
    }

    @GetMapping("/severity-distribution")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<SeverityDistributionResponse> getSeverityDistribution() {
        return incidentService.getSeverityDistribution();
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<TrendResponse> getTrend() {
        return incidentService.getTrend();
    }

    @PostMapping("/{incidentKey}/assign")
    @PreAuthorize("hasRole('ADMIN') ")
    public ApiSuccessResponse assignIncident(
            @PathVariable String incidentKey,
            @Valid @RequestBody AssignRequest request) {
        incidentService.assignIncident(incidentKey, request.getAssignedTo());
        return new ApiSuccessResponse("Incident assigned successfully");
    }
}
