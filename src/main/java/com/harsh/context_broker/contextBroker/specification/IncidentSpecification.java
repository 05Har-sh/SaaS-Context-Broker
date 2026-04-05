package com.harsh.context_broker.contextBroker.specification;

import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.JiraStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import org.springframework.data.jpa.domain.Specification;

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
}
