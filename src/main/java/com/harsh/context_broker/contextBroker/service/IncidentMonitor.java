package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class IncidentMonitor {
    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final SlackNotifier slackNotifier;

    @Value("${staleness.threshold.minutes}")
    private long stalenessThresholdMinutes;

    public IncidentMonitor(IncidentRepository incidentRepository, IncidentService incidentService, SlackNotifier slackNotifier) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.slackNotifier = slackNotifier;
    }

    @Scheduled(fixedRate = 60000)
    public void monitorIncidents() {

        List<IncidentEntity> incidents = incidentRepository.findAll();
        for (IncidentEntity incident : incidents) {

            // Skip resolved/closed incidents — they should not be re-evaluated
            if (incident.getIncidentStatus() == IncidentStatus.RESOLVED
                    || incident.getIncidentStatus() == IncidentStatus.CLOSED) {
                continue;
            }
            LocalDateTime activityTime = incident.getLastActivityAt() != null
                    ?incident.getLastActivityAt()
                    :incident.getLastUpdated();

            if (activityTime == null) {
                continue;
            }

            Long minutesSinceActivity = Duration
                    .between(activityTime, LocalDateTime.now())
                    .toMinutes();
            if (minutesSinceActivity < stalenessThresholdMinutes) {
                continue;
            }

            AlertResponse alert = incidentService.evaluateSeverityPublic(incident);
            if (alert == null || alert.getSeverity() != Severity.CRITICAL) {
                continue;
            }

            LocalDateTime lastEscalated = incident.getLastEscalatedAt();

            boolean cooldownActive = lastEscalated != null && Duration
                    .between(lastEscalated, LocalDateTime.now())
                    .toMinutes() < 5;

            if (cooldownActive) {
                continue;
            }

            String slackMessage = "🚨 CRITICAL INCIDENT: "
                    + incident.getIncidentKey()
                    + "\nScore: " + alert.getScore()
                    + "\nReason: " + alert.getReason();

            slackNotifier.sendAlert(slackMessage);
            incident.setLastEscalatedAt(LocalDateTime.now());
            incidentRepository.save(incident);
        }
    }
}
