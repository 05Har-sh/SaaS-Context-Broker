package com.harsh.context_broker.contextBroker.specification;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class IncidentSpecification {
    public static Specification<IncidentEntity> hasSeverity(Severity severity) {
        return (root, query, cb) ->
                severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    public static Specification<IncidentEntity> hasAssignedTo(String assignedTo) {
        return (root, query, cb) ->
                assignedTo == null ? null :
                        cb.equal(cb.lower(root.get("assignedTo")), assignedTo.toLowerCase());
    }

    public static Specification<IncidentEntity> hasJiraStatus(JiraStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("jiraStatus"), status);
    }

    public static Specification<IncidentEntity> hasIncidentStatus(IncidentStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("incidentStatus"), status);
    }

    public static Specification<IncidentEntity> hasStale(Boolean stale, LocalDateTime cutoff) {
        return (root, query, cb) -> {
            if (stale == null) return null;

            var status = root.<IncidentStatus>get("incidentStatus");
            var lastUpdated = root.<LocalDateTime>get("lastUpdated");

            var terminal = cb.or(
                    cb.equal(status, IncidentStatus.RESOLVED),
                    cb.equal(status, IncidentStatus.CLOSED)
            );

            // isStale == true  => non-terminal AND lastUpdated != null AND lastUpdated <= cutoff
            if (Boolean.TRUE.equals(stale)) {
                return cb.and(
                        cb.not(terminal),
                        cb.isNotNull(lastUpdated),
                        cb.lessThanOrEqualTo(lastUpdated, cutoff)
                );
            }

            // isStale == false => terminal OR lastUpdated == null OR lastUpdated > cutoff
            return cb.or(
                    terminal,
                    cb.isNull(lastUpdated),
                    cb.greaterThan(lastUpdated, cutoff)
            );
        };
    }
}
