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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class IncidentService {
    public final IncidentRepository repository;
    private final IncidentEventRepository eventRepository;
    private final SlackNotifier slackNotifier;
    private final TimelineService timelineService;
    public static final long ALERT_COOLDOWN_MINUTES = 10;

    public IncidentService(IncidentRepository repository,TimelineService timelineService, IncidentEventRepository eventRepository, SlackNotifier slackNotifier) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.slackNotifier = slackNotifier;
        this.timelineService = timelineService;
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
        AtomicBoolean isNew = new AtomicBoolean(false);
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(() -> {
                    isNew.set(true);
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    i.setLastUpdated(LocalDateTime.now());
                    return i;
                });
        if (isNew.get()) {
            timelineService.logEvent(
                    incidentKey,
                    "CREATED",
                    "Incident created via Slack update"
            );
        }
        incident.setLastMsg(message);
        timelineService.logEvent(
                incidentKey,
                "MESSAGE_RECEIVED",
                "Slack message: " + message
        );
        incident.setLastUpdated(LocalDateTime.now());
        AlertResponse alert = evaluateSeverity(incident);
        if (alert != null) {
            timelineService.logEvent(
                    incidentKey,
                    "SEVERITY_EVALUATED",
                    "Score: " + alert.getScore()
                            + ", Severity: "
                            + alert.getSeverity()
            );
        }
        
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

        Severity previousSeverity = incident.getSeverity();

        String message = incident.getLastMsg();
        String jiraStatus = incident.getJiraStatus();
        LocalDateTime lastUpdated = incident.getLastUpdated();

        if(lastUpdated == null){
            incident.setLastUpdated(LocalDateTime.now());
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

        Severity newSeverity = alertResponse.getSeverity();
        incident.setSeverity(newSeverity);

        // ✅ Timeline → Severity evaluated
        timelineService.logEvent(
                incident.getIncidentKey(),
                "SEVERITY_EVALUATED",
                "Score: " + score + ", Severity: " + newSeverity
        );

        // ✅ Timeline → Severity changed
        if (previousSeverity != newSeverity) {
            timelineService.logEvent(
                    incident.getIncidentKey(),
                    "SEVERITY_CHANGED",
                    "Severity changed from "
                            + previousSeverity
                            + " → "
                            + newSeverity
            );
        }

        if((newSeverity == Severity.HIGH || newSeverity == Severity.CRITICAL)
                && canSendAlert(incident)){
            slackNotifier.sendAlert(incident, alertResponse);
            incident.setLastAlertedAt(LocalDateTime.now());

            timelineService.logEvent(
                    incident.getIncidentKey(),
                    "ALERT_SENT",
                    "Slack alert triggered. Severity: "
                            + newSeverity
                            + ", Score: "
                            + score
            );
        } else {
            timelineService.logEvent(
                    incident.getIncidentKey(),
                    "ALERT_SKIPPED",
                    "Cooldown active. Alert suppressed for severity " + newSeverity
            );
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

        MetricsResponse metrics = getMetrics();

        SystemHealthResponse response = new SystemHealthResponse();

        if (metrics.getCritical() > 5) {
            response.setStatus("CRITICAL");
            response.setScore(40);
            response.setReasons(List.of("High number of critical incidents"));
        }
        else if (metrics.getStale() > 10) {
            response.setStatus("DEGRADED");
            response.setScore(70);
            response.setReasons(List.of("Many stale incidents"));
        }
        else {
            response.setStatus("HEALTHY");
            response.setScore(95);
            response.setReasons(List.of("System operating normally"));
        }

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

    public boolean canSendAlert(IncidentEntity incident){
        if(incident.getLastAlertedAt() == null) return true;
        LocalDateTime nextAllowed = incident.getLastAlertedAt().plusMinutes(ALERT_COOLDOWN_MINUTES);
        return LocalDateTime.now().isAfter(nextAllowed);
    }

    public HighestRiskResponse getHighestRisk() {

        List<IncidentEntity> incidents = repository.findAll();

        IncidentEntity highest = incidents.stream()
                .max(Comparator.comparingInt(this::calculateRiskScore))
                .orElse(null);

        if (highest == null) return null;

        HighestRiskResponse response = new HighestRiskResponse();
        response.setIncidentKey(highest.getIncidentKey());
        response.setRiskScore(calculateRiskScore(highest));

        return response;
    }

    private int calculateRiskScore(IncidentEntity incident) {

        if (incident == null) return 0;

        int score = 0;

        String message = incident.getLastMsg();
        String jiraStatus = incident.getJiraStatus();
        LocalDateTime lastUpdated = incident.getLastUpdated();

        // 🔴 Slack urgency
        if (message != null && message.contains("URGENT")) {
            score += urgentWeight; // e.g. 50
        }

        // 🟡 Jira status weighting
        if ("OPEN".equals(jiraStatus)) {
            score += jiraOpenWeight; // e.g. 30
        }
        else if ("IN_PROGRESS".equals(jiraStatus)) {
            score += jiraInProgressWeight; // e.g. 10
        }

        // ⏱ Staleness factor
        if (lastUpdated != null) {

            long minutes = Duration
                    .between(lastUpdated, LocalDateTime.now())
                    .toMinutes();

            if (minutes >= 60) {          // 1 hour stale
                score += staleWeight;     // e.g. 20
            }

            // 🔥 Progressive escalation (optional realism)
            if (minutes >= 180) {         // 3 hours
                score += 10;
            }

            if (minutes >= 360) {         // 6 hours
                score += 20;
            }
        }

        // 🔥 Severity reinforcement (if already computed)
        if (incident.getSeverity() == Severity.CRITICAL) {
            score += 20;
        }
        else if (incident.getSeverity() == Severity.HIGH) {
            score += 10;
        }

        return Math.min(score, 100); // cap at 100
    }

    public List<SeverityDistributionResponse> getSeverityDistribution() {

        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        IncidentEntity::getSeverity,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> {
                    SeverityDistributionResponse r = new SeverityDistributionResponse();
                    r.setSeverity(entry.getKey().name());
                    r.setCount(entry.getValue().intValue());
                    return r;
                })
                .toList();
    }

    public List<TrendResponse> getTrend() {

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);

        // Group incidents by date
        Map<LocalDate, Long> grouped = repository.findAll().stream()
                .filter(i -> i.getPostedAt() != null)
                .map(i -> i.getPostedAt().toLocalDate())
                .filter(date -> !date.isBefore(sevenDaysAgo))
                .collect(Collectors.groupingBy(
                        date -> date,
                        Collectors.counting()
                ));

        List<TrendResponse> result = new ArrayList<>();

        // Ensure all 7 days are present
        for (int i = 0; i < 7; i++) {
            LocalDate date = sevenDaysAgo.plusDays(i);

            TrendResponse response = new TrendResponse();
            response.setDate(date.toString());
            response.setCount(grouped.getOrDefault(date, 0L).intValue());

            result.add(response);
        }

        return result;
    }


}

