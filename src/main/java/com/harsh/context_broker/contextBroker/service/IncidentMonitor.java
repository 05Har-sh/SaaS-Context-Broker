package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
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

    public IncidentMonitor(IncidentRepository incidentRepository, IncidentService incidentService, SlackNotifier slackNotifier){
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.slackNotifier = slackNotifier;
    }

    @Scheduled(fixedRate = 10000)
    public void monitorIncidents(){
        System.out.println("👀 Monitor scanning incidents...");

        List<IncidentEntity> incidents = incidentRepository.findAll();
        for(IncidentEntity incident : incidents){

            if(incident.getLastUpdated() == null){
                continue;
            }

            Long minutesSinceUpdate = Duration
                    .between(incident.getLastUpdated(), LocalDateTime.now())
                    .toMinutes();

            boolean stale = minutesSinceUpdate >= 1;

            if (!stale){
                continue;
            }
            System.out.println("⏱ STALE INCIDENT: "
                    + incident.getIncidentKey()
                    +"stale for "
                    +minutesSinceUpdate + "mins "
            );
            AlertResponse alert = incidentService.evaluateSeverityPublic(incident);

            if (alert == null){
                continue;
            }

            if (alert.getSeverity() == Severity.CRITICAL){
                System.out.println("🚨 AUTO-ESCALATION: "
                        + incident.getIncidentKey()
                        + " | Score: "
                        + alert.getScore());

            }
            LocalDateTime lastEscalated = incident.getLastEscalatedAt();

            boolean cooldownActive = lastEscalated != null && Duration
                    .between(lastEscalated, LocalDateTime.now())
                    .toMinutes() < 5;

            if (cooldownActive){
                System.out.println("Cooldown active for " + incident.getIncidentKey());
                continue;
            }
            String slackMessage =  "🚨 CRITICAL INCIDENT: "
                    + incident.getIncidentKey()
                    + "\nScore: " + alert.getScore()
                    + "\nReason: " + alert.getReason();

            slackNotifier.sendAlert(slackMessage);
            System.out.println( "🚨 CRITICAL INCIDENT: "
                    + incident.getIncidentKey());

            incident.setLastEscalatedAt(LocalDateTime.now());
            incidentRepository.save(incident);

        }
    }
}
