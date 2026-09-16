package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<IncidentEntity, Long>,
        JpaSpecificationExecutor<IncidentEntity> {

    Optional<IncidentEntity> findByTenantIdAndIncidentKey(
            String tenantId,
            String incidentKey
    );

    List<IncidentEntity> findAllByTenantId(String tenantId);

    @Query("SELECT DISTINCT i.tenantId FROM IncidentEntity i")
    List<String> findDistinctTenantIds();

    long count();
    long countBySeverity(Severity severity);
    long countByIncidentStatusNot(IncidentStatus status);

}



