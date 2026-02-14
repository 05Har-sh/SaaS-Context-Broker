package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class IncidentMonitor {
    private final IncidentRepository incidentRepository;

    public IncidentMonitor(IncidentRepository incidentRepository){
        this.incidentRepository = incidentRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void checkForStaleIncidents(){
        System.out.println("⏰ Scheduler running...");
        List<IncidentEntity> incidents = incidentRepository.findAll();
        for(IncidentEntity incident : incidents){
            LocalDateTime lastUpdated = incident.getLastUpdated();

            if(lastUpdated == null) continue;

            Long minutesSinceUpdate = Duration
                    .between(lastUpdated, LocalDateTime.now())
                    .toMinutes();

            //for testing purposes "minutesSinceUpdate > 1" later change it to 30
            if(minutesSinceUpdate > 1){
                System.out.println(
                        "⏱ STALE INCIDENT: " + incident.getIncidentKey()
                                + " not updated for " + minutesSinceUpdate + " mins"
                );
            }
        }

    }
}
