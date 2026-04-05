package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.*;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.exception.ResourceNotFoundException;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import com.harsh.context_broker.contextBroker.specification.IncidentSpecification;
import org.springframework.data.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
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
    private final SlackNotifier slackNotifier;
    private final TimelineService timelineService;
    public static final long ALERT_COOLDOWN_MINUTES = 10;

    public IncidentService(IncidentRepository repository, TimelineService timelineService, SlackNotifier slackNotifier) {
        this.repository = repository;
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

    @Value("${staleness.threshold.minutes}")
    private long stalenessThresholdMinutes;

    // ═══════════════════════════════════════════
    //  INPUT HANDLERS
    // ═══════════════════════════════════════════

    public AlertResponse handleUpdate(String incidentKey, String message) {
        AtomicBoolean isNew = new AtomicBoolean(false);
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(() -> {
                    isNew.set(true);
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    i.setLastUpdated(LocalDateTime.now());
                    i.setIncidentStatus(IncidentStatus.CREATED);
                    return i;
                });

        if (isNew.get()) {
            timelineService.logEvent(incidentKey, "CREATED",
                    "Incident created via Slack update", "SLACK", message);
        }

        incident.setLastMsg(message);
        incident.setLastUpdated(LocalDateTime.now());

        // Unified timeline — replaces old IncidentEventEntity
        timelineService.logEvent(incidentKey, "MESSAGE_RECEIVED",
                "Slack message: " + message, "SLACK", message);

        AlertResponse alert = evaluateSeverity(incident);
        repository.save(incident);
        return alert;
    }

    public AlertResponse handleJiraUpdate(String incidentKey, JiraStatus status) {
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(() -> {
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    i.setLastUpdated(LocalDateTime.now());
                    i.setIncidentStatus(IncidentStatus.CREATED);
                    return i;
                });

        incident.setJiraStatus(status);
        // Bug 5 fix: set lastUpdated BEFORE evaluating severity
        incident.setLastUpdated(LocalDateTime.now());

        // Map Jira status → incident lifecycle status
        switch (status) {
            case OPEN -> incident.setIncidentStatus(IncidentStatus.CREATED);
            case IN_PROGRESS -> incident.setIncidentStatus(IncidentStatus.IN_PROGRESS);
            case RESOLVED -> {
                incident.setIncidentStatus(IncidentStatus.RESOLVED);
                if (incident.getResolvedAt() == null) {
                    incident.setResolvedAt(LocalDateTime.now());
                }
            }
            case CLOSED -> {
                incident.setIncidentStatus(IncidentStatus.CLOSED);
                if (incident.getResolvedAt() == null) {
                    incident.setResolvedAt(LocalDateTime.now());
                }
            }
        }

        // Unified timeline — replaces old IncidentEventEntity
        timelineService.logEvent(incidentKey, "JIRA_UPDATE",
                "Jira status changed to " + status, "JIRA", "Status: " + status);

        AlertResponse alert = evaluateSeverity(incident);
        repository.save(incident);
        return alert;
    }

    // ═══════════════════════════════════════════
    //  SEVERITY ENGINE (Core Intelligence)
    // ═══════════════════════════════════════════

    private AlertResponse evaluateSeverity(IncidentEntity incident) {

        // Resolved/Closed → score 0, keep existing severity (severity = impact, status = lifecycle)
        if (incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED) {
            AlertResponse response = new AlertResponse();
            response.setScore(0);
            response.setReason("Incident " + incident.getIncidentStatus().name().toLowerCase());
            response.setSeverity(incident.getSeverity() != null ? incident.getSeverity() : Severity.LOW);

            timelineService.logEvent(incident.getIncidentKey(),
                    incident.getIncidentStatus().name(),
                    "Incident " + incident.getIncidentStatus().name().toLowerCase() + ". Score reset to 0.");
            return response;
        }

        Severity previousSeverity = incident.getSeverity();
        String message = incident.getLastMsg();
        LocalDateTime lastUpdated = incident.getLastUpdated();

        if (lastUpdated == null) {
            incident.setLastUpdated(LocalDateTime.now());
            lastUpdated = incident.getLastUpdated();
        }

        boolean urgent = message != null && message.contains("URGENT");
        long minutesSinceUpdate = Duration.between(lastUpdated, LocalDateTime.now()).toMinutes();
        boolean stale = minutesSinceUpdate >= stalenessThresholdMinutes;

        int score = 0;
        String reason = "";

        if (urgent) {
            score += urgentWeight;
            reason += "Slack marked URGENT. ";
        }
        if (incident.getJiraStatus() == JiraStatus.OPEN) {
            score += jiraOpenWeight;
            reason += "Jira still OPEN. ";
        }
        if (incident.getJiraStatus() == JiraStatus.IN_PROGRESS) {
            score += jiraInProgressWeight;
            reason += "Jira IN_PROGRESS. ";
        }
        if (stale) {
            score += staleWeight;
            reason += "Incident stale for " + minutesSinceUpdate + " mins. ";
        }

        AlertResponse alertResponse = new AlertResponse();
        alertResponse.setScore(score);
        alertResponse.setReason(reason);

        // Severity thresholds with new MEDIUM level
        if (score >= 70) {
            alertResponse.setSeverity(Severity.CRITICAL);
        } else if (score >= 50) {
            alertResponse.setSeverity(Severity.HIGH);
        } else if (score >= 30) {
            alertResponse.setSeverity(Severity.MEDIUM);
        } else {
            alertResponse.setSeverity(Severity.LOW);
        }

        Severity newSeverity = alertResponse.getSeverity();
        incident.setSeverity(newSeverity);

        timelineService.logEvent(incident.getIncidentKey(),
                "SEVERITY_EVALUATED", "Score: " + score + ", Severity: " + newSeverity);

        if (previousSeverity != newSeverity) {
            timelineService.logEvent(incident.getIncidentKey(),
                    "SEVERITY_CHANGED",
                    "Severity changed from " + previousSeverity + " → " + newSeverity);
        }

        if ((newSeverity == Severity.HIGH || newSeverity == Severity.CRITICAL)
                && canSendAlert(incident)) {
            slackNotifier.sendAlert(incident, alertResponse);
            incident.setLastAlertedAt(LocalDateTime.now());
            timelineService.logEvent(incident.getIncidentKey(),
                    "ALERT_SENT",
                    "Slack alert triggered. Severity: " + newSeverity + ", Score: " + score);
        } else {
            timelineService.logEvent(incident.getIncidentKey(),
                    "ALERT_SKIPPED",
                    "Cooldown active. Alert suppressed for severity " + newSeverity);
        }

        return alertResponse;
    }

    public AlertResponse evaluateSeverityPublic(IncidentEntity incident) {
        return evaluateSeverity(incident);
    }

    // ═══════════════════════════════════════════
    //  READ OPERATIONS (no side effects)
    // ═══════════════════════════════════════════

    public IncidentEntity getIncidentByKey(String incidentKey) {
        return repository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));
    }

    /**
     * Returns a single incident as a response DTO (used by controller).
     */
    public IncidentResponse getIncidentResponse(String incidentKey) {
        IncidentEntity incident = getIncidentByKey(incidentKey);
        return mapToResponse(incident);
    }

    public Page<IncidentResponse> getAllIncidents(int page, int size,
                                                  Severity severity, String assignedTo,
                                                  Boolean stale, JiraStatus jiraStatus) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastUpdated").descending());

        Specification<IncidentEntity> spec = Specification
                .where(IncidentSpecification.hasSeverity(severity))
                .and(IncidentSpecification.hasAssignedTo(assignedTo))
                .and(IncidentSpecification.hasJiraStatus(jiraStatus));

        Page<IncidentEntity> incidentsPage = repository.findAll(spec, pageable);

        List<IncidentResponse> content = incidentsPage.getContent().stream()
                .filter(i -> stale == null || isStale(i) == stale)
                .map(this::mapToResponse)
                .toList();

        return new PageImpl<>(content, pageable, incidentsPage.getTotalElements());
    }

    private IncidentResponse mapToResponse(IncidentEntity incident) {
        IncidentResponse response = new IncidentResponse();
        response.setIncidentKey(incident.getIncidentKey());
        response.setLastMsg(incident.getLastMsg());
        response.setJiraStatus(incident.getJiraStatus() != null ? incident.getJiraStatus().name() : null);
        response.setIncidentStatus(incident.getIncidentStatus() != null ? incident.getIncidentStatus().name() : null);

        if (incident.getLastUpdated() != null) {
            response.setLastUpdated(incident.getLastUpdated().toString());
            response.setStale(isStale(incident));
        }

        boolean isTerminal = incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED;

        response.setSeverity(incident.getSeverity() != null ? incident.getSeverity() : Severity.LOW);
        response.setRiskScore(isTerminal ? 0 : calculateRiskScore(incident));
        response.setAssignedTo(incident.getAssignedTo() != null ? incident.getAssignedTo() : "UNASSIGNED");

        return response;
    }

    /**
     * Read-only — does NOT re-evaluate severity.
     */
    public IncidentDetailsResponse getIncidentDetails(String incidentKey) {
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));

        boolean isTerminal = incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED;

        IncidentDetailsResponse dto = new IncidentDetailsResponse();
        dto.setIncidentKey(incident.getIncidentKey());
        dto.setLastMsg(incident.getLastMsg());
        dto.setJiraStatus(incident.getJiraStatus() != null ? incident.getJiraStatus().name() : null);
        dto.setIncidentStatus(incident.getIncidentStatus() != null ? incident.getIncidentStatus().name() : null);
        dto.setSeverity(incident.getSeverity() != null ? incident.getSeverity().name() : Severity.LOW.name());
        dto.setLastUpdated(incident.getLastUpdated() != null ? incident.getLastUpdated().toString() : null);
        dto.setPostedAt(incident.getPostedAt() != null ? incident.getPostedAt().toString() : null);
        dto.setRiskScore(isTerminal ? 0 : calculateRiskScore(incident));
        dto.setAssignedTo(incident.getAssignedTo() != null ? incident.getAssignedTo() : "UNASSIGNED");

        return dto;
    }

    // ═══════════════════════════════════════════
    //  METRICS & ANALYTICS
    // ═══════════════════════════════════════════

    public MetricsResponse getMetrics() {
        List<IncidentEntity> incidents = repository.findAll();

        long total = incidents.size();

        long active = incidents.stream()
                .filter(i -> i.getIncidentStatus() != IncidentStatus.RESOLVED
                        && i.getIncidentStatus() != IncidentStatus.CLOSED)
                .count();

        long critical = incidents.stream()
                .filter(i -> Severity.CRITICAL.equals(i.getSeverity()))
                .filter(i -> i.getIncidentStatus() != IncidentStatus.RESOLVED
                        && i.getIncidentStatus() != IncidentStatus.CLOSED)
                .count();

        long stale = incidents.stream()
                .filter(this::isStale)
                .count();

        long resolved = incidents.stream()
                .filter(i -> i.getIncidentStatus() == IncidentStatus.RESOLVED
                        || i.getIncidentStatus() == IncidentStatus.CLOSED)
                .count();

        MetricsResponse response = new MetricsResponse();
        response.setTotal(total);
        response.setActive(active);
        response.setCritical(critical);
        response.setStale(stale);
        response.setResolved(resolved);
        response.setEscalationsToday(0);

        return response;
    }

    private boolean isStale(IncidentEntity incident) {
        // Resolved/closed incidents are never stale
        if (incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED) {
            return false;
        }
        if (incident.getLastUpdated() == null) return false;

        long minutes = Duration.between(incident.getLastUpdated(), LocalDateTime.now()).toMinutes();
        return minutes >= stalenessThresholdMinutes;
    }

    public SystemHealthResponse getSystemHealth() {
        MetricsResponse metrics = getMetrics();
        SystemHealthResponse response = new SystemHealthResponse();

        if (metrics.getCritical() > 5) {
            response.setStatus("CRITICAL");
            response.setScore(40);
            response.setReasons(List.of("High number of critical incidents"));
        } else if (metrics.getStale() > 10) {
            response.setStatus("DEGRADED");
            response.setScore(70);
            response.setReasons(List.of("Many stale incidents"));
        } else {
            response.setStatus("HEALTHY");
            response.setScore(95);
            response.setReasons(List.of("System operating normally"));
        }

        return response;
    }

    // ═══════════════════════════════════════════
    //  RISK & PRIORITY
    // ═══════════════════════════════════════════

    public RiskScoreResponse getRiskScore(String incidentKey) {
        IncidentEntity incident = getIncidentByKey(incidentKey);

        if (incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED) {
            RiskScoreResponse response = new RiskScoreResponse();
            response.setIncidentKey(incidentKey);
            response.setRiskScore(0);
            response.setReason("Incident " + incident.getIncidentStatus().name().toLowerCase() + ".");
            return response;
        }

        int score = 0;
        String reason = "";

        if (incident.getLastMsg() != null && incident.getLastMsg().contains("URGENT")) {
            score += 40;
            reason += "URGENT detected. ";
        }
        if (incident.getJiraStatus() == JiraStatus.OPEN) {
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

        if (incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED) {
            priority = "RESOLVED";
        } else if (Severity.CRITICAL.equals(incident.getSeverity())) {
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

    public boolean canSendAlert(IncidentEntity incident) {
        if (incident.getLastAlertedAt() == null) return true;
        LocalDateTime nextAllowed = incident.getLastAlertedAt().plusMinutes(ALERT_COOLDOWN_MINUTES);
        return LocalDateTime.now().isAfter(nextAllowed);
    }

    public HighestRiskResponse getHighestRisk() {
        List<IncidentEntity> incidents = repository.findAll();

        IncidentEntity highest = incidents.stream()
                .filter(i -> i.getIncidentStatus() != IncidentStatus.RESOLVED
                        && i.getIncidentStatus() != IncidentStatus.CLOSED)
                .max(Comparator.comparingInt(this::calculateRiskScore))
                .orElse(null);

        if (highest == null) return null;

        HighestRiskResponse response = new HighestRiskResponse();
        response.setIncidentKey(highest.getIncidentKey());
        response.setRiskScore(calculateRiskScore(highest));
        return response;
    }

    // Bug 4 fix: null check BEFORE dereferencing
    private int calculateRiskScore(IncidentEntity incident) {
        if (incident == null) return 0;

        if (incident.getIncidentStatus() == IncidentStatus.RESOLVED
                || incident.getIncidentStatus() == IncidentStatus.CLOSED) {
            return 0;
        }

        int score = 0;
        String message = incident.getLastMsg();
        LocalDateTime lastUpdated = incident.getLastUpdated();

        if (message != null && message.contains("URGENT")) {
            score += urgentWeight;
        }

        if (incident.getJiraStatus() == JiraStatus.OPEN) {
            score += jiraOpenWeight;
        } else if (incident.getJiraStatus() == JiraStatus.IN_PROGRESS) {
            score += jiraInProgressWeight;
        }

        if (lastUpdated != null) {
            long minutes = Duration.between(lastUpdated, LocalDateTime.now()).toMinutes();
            if (minutes >= 60) score += staleWeight;
            if (minutes >= 180) score += 10;
            if (minutes >= 360) score += 20;
        }

        if (incident.getSeverity() == Severity.CRITICAL) {
            score += 20;
        } else if (incident.getSeverity() == Severity.HIGH) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    // ═══════════════════════════════════════════
    //  DISTRIBUTION & TREND
    // ═══════════════════════════════════════════

    public List<SeverityDistributionResponse> getSeverityDistribution() {
        return repository.findAll().stream()
                .filter(i -> i.getSeverity() != null)
                .filter(i -> i.getIncidentStatus() != IncidentStatus.RESOLVED
                        && i.getIncidentStatus() != IncidentStatus.CLOSED)
                .collect(Collectors.groupingBy(IncidentEntity::getSeverity, Collectors.counting()))
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

        Map<LocalDate, Long> grouped = repository.findAll().stream()
                .filter(i -> i.getPostedAt() != null)
                .map(i -> i.getPostedAt().toLocalDate())
                .filter(date -> !date.isBefore(sevenDaysAgo))
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));

        List<TrendResponse> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = sevenDaysAgo.plusDays(i);
            TrendResponse response = new TrendResponse();
            response.setDate(date.toString());
            response.setCount(grouped.getOrDefault(date, 0L).intValue());
            result.add(response);
        }
        return result;
    }

    // ═══════════════════════════════════════════
    //  ASSIGNMENT
    // ═══════════════════════════════════════════

    public void assignIncident(String incidentKey, String assignedTo) {
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));
        String previousAssignee = incident.getAssignedTo();

        if (assignedTo.equalsIgnoreCase(previousAssignee)) {
            return;
        }

        incident.setAssignedTo(assignedTo);
        incident.setLastUpdated(LocalDateTime.now());
        repository.save(incident);

        if (previousAssignee == null) {
            timelineService.logEvent(incidentKey, "ASSIGNED", "Assigned to " + assignedTo);
        } else {
            timelineService.logEvent(incidentKey, "REASSIGNED",
                    "Reassigned from " + previousAssignee + " to " + assignedTo);
        }
    }
}
