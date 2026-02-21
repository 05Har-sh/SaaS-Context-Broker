package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.IncidentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentEventRepository extends JpaRepository<IncidentEventEntity, Long> {
    List<IncidentEventEntity> findByIncidentKeyOrderByTimestampAsc(String incidentKey);
}
