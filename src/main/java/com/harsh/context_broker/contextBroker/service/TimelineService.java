package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.TimelineEventResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEventEntity;
import com.harsh.context_broker.contextBroker.entity.TimelineEventEntity;
import com.harsh.context_broker.contextBroker.repository.IncidentEventRepository;
import com.harsh.context_broker.contextBroker.repository.TimelineEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimelineService {

    private final TimelineEventRepository repository;
    private final IncidentEventRepository legacyRepository;

    public TimelineService(TimelineEventRepository repository, IncidentEventRepository legacyRepository) {
        this.repository = repository;
        this.legacyRepository = legacyRepository;
    }

    /**
     * Log a system-generated event (source defaults to SYSTEM).
     */
    public void logEvent(String incidentKey, String type, String description) {
        logEvent(incidentKey, type, description, "SYSTEM", null);
    }

    /**
     * Log an event with explicit source and content (for Slack/Jira events).
     */
    public void logEvent(String incidentKey, String type, String description, String source, String content) {
        TimelineEventEntity event = new TimelineEventEntity();
        event.setIncidentKey(incidentKey);
        event.setEventType(type);
        event.setDescription(description);
        event.setSource(source);
        event.setContent(content);
        event.setTimestamp(LocalDateTime.now());

        repository.save(event);
    }

    public List<TimelineEventResponse> getTimelineForIncident(String incidentKey) {
        List<TimelineEventResponse> newEvents = repository.findByIncidentKeyOrderByTimestampDesc(incidentKey)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        List<TimelineEventResponse> legacyEvents = legacyRepository.findByIncidentKeyOrderByTimestampAsc(incidentKey)
                .stream()
                .map(this::mapLegacyToResponse)
                .collect(Collectors.toList());

        List<TimelineEventResponse> allEvents = new ArrayList<>();
        allEvents.addAll(newEvents);
        allEvents.addAll(legacyEvents);

        // Sort descending by looking at the raw string timestamp or we can sort by comparing string directly.
        // But since it's ISO string, reverse string comparison is fine. 
        // More robust: sort them in the stream before mapping, or just string compare.
        allEvents.sort((a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return allEvents;
    }

    private TimelineEventResponse mapToResponse(TimelineEventEntity event) {
        TimelineEventResponse response = new TimelineEventResponse();
        response.setEventType(event.getEventType());
        response.setDescription(event.getDescription());
        response.setSource(event.getSource());
        response.setContent(event.getContent());
        response.setTimestamp(event.getTimestamp() != null ? event.getTimestamp().toString() : null);
        return response;
    }

    private TimelineEventResponse mapLegacyToResponse(IncidentEventEntity legacyEvent) {
        TimelineEventResponse response = new TimelineEventResponse();
        response.setEventType("LEGACY_EVENT");
        response.setDescription("Legacy event: " + legacyEvent.getSource());
        response.setSource(legacyEvent.getSource());
        response.setContent(legacyEvent.getContent());
        response.setTimestamp(legacyEvent.getTimestamp() != null ? legacyEvent.getTimestamp().toString() : null);
        return response;
    }
}
