package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class IncidentService {
    public final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
    }

    public String handleUpdate(String incidentKey, String message) {
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(() -> {
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    return i;
                });
        incident.setLastMsg(message);
        incident.setLastUpdated(LocalDateTime.now());

        repository.save(incident);
        return evaluateAlert(incident);
    }

    public IncidentEntity getIncidentByKey(String incidentKey) {
        return repository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new RuntimeException("Incident not found"));
    }

    private String evaluateAlert(IncidentEntity incident) {

        String message = incident.getLastMsg();
        String jiraStatus = incident.getJiraStatus();
        LocalDateTime lastUpdated = incident.getLastUpdated();

        if(message == null || lastUpdated == null){
            return null;
        }
        boolean urgent = message.contains("URGENT");

        long minutesSinceUpdate = Duration
                .between(lastUpdated, LocalDateTime.now())
                .toMinutes();
        boolean stale = minutesSinceUpdate >= 1; //keep 1 minute for testing

        if (urgent) {
            // 🚨 RULE 1: URGENT + Jira null → Immediate Escalation
            if(jiraStatus == null){
                return "🚨ESCALATION: Slack marked URGENT but Jira has no Status yet ->" + incident.getIncidentKey();
            }
            // 🚨 RULE 2: URGENT + Jira OPEN → Immediate Escalation
            if("OPEN".equals(jiraStatus)){
                return "🚨ESCALATION: Slack marked URGENT but Jira still OPEN ->" + incident.getIncidentKey();
            }
        }
        // 🚨 RULE 3: URGENT + IN_PROGRESS + stale → Re-Escalation
        if (urgent && "IN_PROGRESS".equals(jiraStatus) && stale){
            return "🚨 RE-ESCALATION: Incident IN_PROGRESS but stale for "
                    + minutesSinceUpdate + " mins -> "
                    + incident.getIncidentKey();
        }
//        System.out.println("--------Alert debug-------");
//        System.out.println("message"+message);
//        System.out.println("jira status = "+ jiraStatus);
//        System.out.println("urgent = "+urgent);
//        System.out.println("minutes = "+minutesSinceUpdate);
//        System.out.println("stale = "+stale);
//        System.out.println("---------------------------");
        return null;
    }

    public String handleJiraUpdate(String incidentKey, String status){
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(()->{
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    return i;
                });
        incident.setJiraStatus(status);
        String alert = evaluateAlert(incident);
        incident.setLastUpdated(LocalDateTime.now());
        repository.save(incident);
        return alert;
    }
}

