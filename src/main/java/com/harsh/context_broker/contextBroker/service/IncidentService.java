package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.*;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.entity.IncidentEventEntity;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.repository.IncidentEventRepository;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {
    public final IncidentRepository repository;
    private final IncidentEventRepository eventRepository;

    public IncidentService(IncidentRepository repository, IncidentEventRepository eventRepository) {
        this.repository = repository;
        this.eventRepository = eventRepository;
    }
    @Value("${scoring.urgent}")
    private int urgentWeight;

    @Value("${scoring.jiraOpen}")
    private int jiraOpenWeight;

    @Value("${scoring.jiraInProgress}")
    private int jiraInProgressWeight;

    @Value("${scoring.stale}")
    private int staleWeight;


    public AlertResponse handleUpdate(String incidentKey, String message) {
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(() -> {
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    return i;
                });
        incident.setLastMsg(message);
        AlertResponse alert = evaluateSeverity(incident);
        incident.setLastUpdated(LocalDateTime.now());

        IncidentEventEntity event = new IncidentEventEntity();
        event.setIncidentKey(incidentKey);
        event.setSource("SLACK");
        event.setContent(message);
        event.setTimestamp(LocalDateTime.now());
        eventRepository.save(event);

        repository.save(incident);
        return alert;
    }

    public IncidentEntity getIncidentByKey(String incidentKey) {
        return repository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
    }

    private AlertResponse evaluateSeverity(IncidentEntity incident) {

        String message = incident.getLastMsg();
        String jiraStatus = incident.getJiraStatus();
        LocalDateTime lastUpdated = incident.getLastUpdated();

        if(lastUpdated == null){
            return null;
        }
        boolean urgent = message != null && message.contains("URGENT");

        long minutesSinceUpdate = Duration
                .between(lastUpdated, LocalDateTime.now())
                .toMinutes();
        boolean stale = minutesSinceUpdate >= 1; //keep 1 minute for testing

        AlertResponse alertResponse = new AlertResponse();
        int score = 0;
        String reason = "";
        if(urgent){
            score += urgentWeight;
            reason += "Slack marked URGENT. ";
        }
        if("OPEN".equals(jiraStatus)){
            score += jiraOpenWeight;
            reason += "Jira still OPEN. ";
        }
        if("IN_PROGRESS".equals(jiraStatus)){
            score += jiraInProgressWeight;
            reason += "Jira IN_PROGRESS. ";
        }
        if(stale){
            score += staleWeight;
            reason += "Incident stale for " + minutesSinceUpdate + " mins. ";
        }
        alertResponse.setScore(score);
        alertResponse.setReason(reason);

        if(score >= 70){
            alertResponse.setSeverity(Severity.CRITICAL);
        } else if (score >= 40) {
            alertResponse.setSeverity(Severity.HIGH);
        } else{
            alertResponse.setSeverity(Severity.LOW);
        }
        return alertResponse;
    }

    public AlertResponse handleJiraUpdate(String incidentKey, String status){
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(()->{
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    return i;
                });
        incident.setJiraStatus(status);

        IncidentEventEntity event = new IncidentEventEntity();
        event.setIncidentKey(incidentKey);
        event.setSource("JIRA");
        event.setContent("Status changed to " + status);
        event.setTimestamp(LocalDateTime.now());
        eventRepository.save(event);

        AlertResponse alert = evaluateSeverity(incident);
        incident.setLastUpdated(LocalDateTime.now());
        repository.save(incident);
        return alert;
    }
    public AlertResponse evaluateSeverityPublic(IncidentEntity incident){
        return evaluateSeverity(incident);
    }

    public List<IncidentResponse> getAllIncidents() {

        List<IncidentEntity> incidents = repository.findAll();

        return incidents.stream().map(incident -> {

            IncidentResponse response = new IncidentResponse();
            response.setIncidentKey(incident.getIncidentKey());
            response.setLastMsg(incident.getLastMsg());
            response.setJiraStatus(incident.getJiraStatus());

            if (incident.getLastUpdated() != null) {
                response.setLastUpdated(incident.getLastUpdated().toString());

                boolean stale = Duration
                        .between(incident.getLastUpdated(), LocalDateTime.now())
                        .toMinutes() >= 1;

                response.setStale(stale);
            }

            return response;

        }).toList();
    }

    public List<TimelineEventResponse> getTimeLine(String incidentKey){
        List<IncidentEventEntity> events = eventRepository.findByIncidentKeyOrderByTimestampAsc(incidentKey);
         return events.stream().map(event ->{
             TimelineEventResponse dto = new TimelineEventResponse();
             dto.setSource(event.getSource());
             dto.setContent(event.getContent());
             dto.setTimeStamp(event.getTimestamp());
             return dto;
         }).toList();
    }

    public IncidentDetailsResponse getIncidentDetails(String incidentKey){
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseThrow(()->new RuntimeException("Incident not found"));
        AlertResponse alert = evaluateSeverity(incident);
        IncidentDetailsResponse dto = new IncidentDetailsResponse();
        dto.setIncidentKey(incident.getIncidentKey());
        dto.setLastMsg(incident.getLastMsg());
        dto.setJiraStatus(incident.getJiraStatus());
        dto.setSeverity(alert.getSeverity().name());
        dto.setLastUpdated(
                incident.getLastUpdated() != null
                ? incident.getLastUpdated().toString()
                        : null
        );
        dto.setPostedAt(
                incident.getPostedAt() != null
                ? incident.getPostedAt().toString()
                        : null
        );
        return dto;
    }

    public MetricsResponse getMetrics(){
        List<IncidentEntity> incidents = repository.findAll();

        long total = incidents.size();
        long active = incidents.stream()
                .filter(i -> !"RESOLVED".equals(i.getJiraStatus()))
                .count();

        long critical = incidents.stream()
                .filter(i -> "CRITICAL".equals(i.getSeverity()))
                .count();

        long stale = incidents.stream()
                .filter(this::isStale)
                .count();

        MetricsResponse response = new MetricsResponse();
        response.setTotal(total);
        response.setActive(active);
        response.setCritical(critical);
        response.setStale(stale);
        response.setEscalationsToday(0); // later when alerts table exists

        return response;
    }

    private boolean isStale(IncidentEntity incident) {
        if (incident.getLastUpdated() == null) return false;

        long minutes = Duration
                .between(incident.getLastUpdated(), LocalDateTime.now())
                .toMinutes();

        return minutes >= 1; // adjust later
    }

    public SystemHealthResponse getSystemHealth() {

        List<IncidentEntity> incidents = repository.findAll();

        long total = incidents.size();
        long stale = incidents.stream().filter(this::isStale).count();
        long critical = incidents.stream()
                .filter(i -> Severity.CRITICAL.equals(i.getSeverity()))
                .count();

        String status = "HEALTHY";

        if (critical > 0) status = "CRITICAL";
        else if (stale > 2) status = "WARNING";

        SystemHealthResponse response = new SystemHealthResponse();
        response.setStatus(status);
        response.setTotalIncidents(total);
        response.setStaleIncidents(stale);
        response.setCriticalIncidents(critical);

        return response;
    }

    public RiskScoreResponse getRiskScore(String incidentKey) {

        IncidentEntity incident = getIncidentByKey(incidentKey);

        int score = 0;
        String reason = "";

        if (incident.getLastMsg() != null && incident.getLastMsg().contains("URGENT")) {
            score += 40;
            reason += "URGENT detected. ";
        }

        if ("OPEN".equals(incident.getJiraStatus())) {
            score += 30;
            reason += "Jira OPEN. ";
        }

        if (isStale(incident)) {
            score += 20;
            reason += "Incident stale. ";
        }

        if (Severity.CRITICAL.equals(incident.getSeverity())) {
            score += 50;
            reason += "Severity CRITICAL. ";
        }

        RiskScoreResponse response = new RiskScoreResponse();
        response.setIncidentKey(incidentKey);
        response.setRiskScore(score);
        response.setReason(reason);

        return response;
    }

    public PriorityResponse getPriority(String incidentKey) {

        IncidentEntity incident = getIncidentByKey(incidentKey);

        String priority = "LOW";

        if (Severity.CRITICAL.equals(incident.getSeverity())) {
            priority = "P1";
        } else if (Severity.HIGH.equals(incident.getSeverity())) {
            priority = "HIGH";
        } else if (isStale(incident)) {
            priority = "MEDIUM";
        }

        PriorityResponse response = new PriorityResponse();
        response.setIncidentKey(incidentKey);
        response.setPriority(priority);

        return response;
    }


}

