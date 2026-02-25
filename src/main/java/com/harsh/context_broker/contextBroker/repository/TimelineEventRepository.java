package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.TimelineEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimelineEventRepository extends JpaRepository<TimelineEventEntity, Long> {
    List<TimelineEventEntity> findByIncidentKeyOrderByTimestampDesc(String incidentKey);

}
