package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

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
        String alert = null;
        String message = incident.getLastMsg();

        if (message != null && message.contains("URGENT")) {
            alert = "🚨 ALERT: Urgent incident detected: " + incident.getIncidentKey();
        }
        return alert;
    }
}

