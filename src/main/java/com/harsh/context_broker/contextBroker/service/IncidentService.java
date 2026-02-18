package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class IncidentService {
    public final IncidentRepository repository;

    public IncidentService(IncidentRepository repository) {
        this.repository = repository;
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


// -----------------------------------old logic---------------------------------------------
//        if (urgent) {
//            // 🚨 RULE 1: URGENT + Jira null → Immediate Escalation
//            if(jiraStatus == null){
//                return "🚨ESCALATION: Slack marked URGENT but Jira has no Status yet ->" + incident.getIncidentKey();
//            }
//            // 🚨 RULE 2: URGENT + Jira OPEN → Immediate Escalation
//            if("OPEN".equals(jiraStatus)){
//                return "🚨ESCALATION: Slack marked URGENT but Jira still OPEN ->" + incident.getIncidentKey();
//            }
//        }
//        // 🚨 RULE 3: URGENT + IN_PROGRESS + stale → Re-Escalation
//        if (urgent && "IN_PROGRESS".equals(jiraStatus) && stale){
//            return "🚨 RE-ESCALATION: Incident IN_PROGRESS but stale for "
//                    + minutesSinceUpdate + " mins -> "
//                    + incident.getIncidentKey();
//        }


//--------------------------------debug-----------------------------------------------------------
//        System.out.println("--------Alert debug-------");
//        System.out.println("message"+message);
//        System.out.println("jira status = "+ jiraStatus);
//        System.out.println("urgent = "+urgent);
//        System.out.println("minutes = "+minutesSinceUpdate);
//        System.out.println("stale = "+stale);
//        System.out.println("---------------------------");


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
        AlertResponse alert = evaluateSeverity(incident);
        incident.setLastUpdated(LocalDateTime.now());
        repository.save(incident);
        return alert;
    }
}

