package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.stereotype.Service;

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

        if (message != null && message.contains("URGENT")) {
            if(jiraStatus == null){
                return "🚨ESCALATION: Slack marked URGENT but Jira has no Status yet ->" + incident.getIncidentKey();
            }
            if("OPEN".equals(jiraStatus)){
                return "🚨ESCALATION: Slack marked URGENT but Jira still OPEN ->" + incident.getIncidentKey();
            }
        }
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
        incident.setLastUpdated(LocalDateTime.now());
        repository.save(incident);
        return evaluateAlert(incident);
    }
}

