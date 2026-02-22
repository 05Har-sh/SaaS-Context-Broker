package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    Optional<IncidentEntity> findByIncidentKey(String incidentKey);
    long count();

    long countBySeverity(String severity);

    long countByJiraStatusNot(String status);
}
