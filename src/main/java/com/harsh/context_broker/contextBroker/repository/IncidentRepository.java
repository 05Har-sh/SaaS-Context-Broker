package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long>,
        JpaSpecificationExecutor<IncidentEntity> {
    Optional<IncidentEntity> findByIncidentKey(String incidentKey);
    long count();
    long countBySeverity(Severity severity);
    long countByIncidentStatusNot(IncidentStatus status);
}
