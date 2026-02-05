package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class IncidentService {
    public final IncidentRepository repository;

    public IncidentService(IncidentRepository repository){
        this.repository = repository;
    }
    public void handleUpdate(String incidentKey, String message){
        IncidentEntity incident = repository.findByIncidentKey(incidentKey)
                .orElseGet(()->{
                    IncidentEntity i = new IncidentEntity();
                    i.setIncidentKey(incidentKey);
                    i.setPostedAt(LocalDateTime.now());
                    return i;
                });
        incident.setLastMsg(message);
        incident.setLastUpdated(LocalDateTime.now());

        repository.save(incident);

    }
    public IncidentEntity getIncidentByKey(String incidentKey){
        return repository.findByIncidentKey(incidentKey)
                .orElseThrow(()-> new RuntimeException("Incident not found"));
    }
}
