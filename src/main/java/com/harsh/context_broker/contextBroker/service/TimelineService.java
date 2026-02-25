package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.TimelineEventResponse;
import com.harsh.context_broker.contextBroker.entity.TimelineEventEntity;
import com.harsh.context_broker.contextBroker.repository.TimelineEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimelineService {

    private final TimelineEventRepository repository;

    public TimelineService(TimelineEventRepository repository) {
        this.repository = repository;
    }

    public void logEvent(String incidentKey, String type, String description) {

        TimelineEventEntity event = new TimelineEventEntity();
        event.setIncidentKey(incidentKey);
        event.setEventType(type);
        event.setDescription(description);
        event.setTimestamp(LocalDateTime.now());

        repository.save(event);
    }

    // ✅ THIS is what was missing
    public List<TimelineEventResponse> getTimelineForIncident(String incidentKey) {

        return repository.findByIncidentKeyOrderByTimestampDesc(incidentKey)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private TimelineEventResponse mapToResponse(TimelineEventEntity event) {

        TimelineEventResponse response = new TimelineEventResponse();
        response.setEventType(event.getEventType());
        response.setDescription(event.getDescription());
        response.setTimeStamp(event.getTimestamp());

        return response;
    }
}
