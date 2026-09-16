package com.harsh.context_broker.contextBroker;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import com.harsh.context_broker.contextBroker.service.IncidentMonitor;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import com.harsh.context_broker.contextBroker.service.SlackNotifier;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.test.context.TestConfiguration;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"staleness.threshold.minutes=10"})
class IncidentMonitorTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentMonitor incidentMonitor;

    private IncidentService incidentService;

    private SlackNotifier slackNotifier;

    @BeforeEach
    void setUp() throws Exception {
        TenantContext.clear();
        incidentRepository.deleteAll();

        this.incidentService = Mockito.mock(IncidentService.class);
        this.slackNotifier = Mockito.mock(SlackNotifier.class);

        // Inject mocks into the IncidentMonitor instance using reflection
        var cls = incidentMonitor.getClass();
        var svcField = cls.getDeclaredField("incidentService");
        svcField.setAccessible(true);
        svcField.set(incidentMonitor, this.incidentService);

        var notifyField = cls.getDeclaredField("slackNotifier");
        notifyField.setAccessible(true);
        notifyField.set(incidentMonitor, this.slackNotifier);
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        incidentRepository.deleteAll();
    }

    @Test
    void shouldEscalateCriticalIncidentForTenant() {
        IncidentEntity incident = new IncidentEntity();
        incident.setTenantId("tenant-1");
        incident.setIncidentKey("INC-ESC-1");
        incident.setIncidentStatus(IncidentStatus.IN_PROGRESS);
        incident.setLastActivityAt(LocalDateTime.now().minusMinutes(30)); // stale
        incidentRepository.saveAndFlush(incident);

        AlertResponse alert = new AlertResponse();
        alert.setSeverity(Severity.CRITICAL);
        alert.setScore(90);
        alert.setReason("stale");

        when(incidentService.evaluateSeverityPublic(any())).thenAnswer(invocation -> {
            IncidentEntity arg = invocation.getArgument(0);
            return "INC-ESC-1".equals(arg.getIncidentKey()) ? alert : null;
        });

        incidentMonitor.monitorIncidents();

        verify(slackNotifier, times(1)).sendAlert(anyString());

        IncidentEntity updated = incidentRepository.findByTenantIdAndIncidentKey("tenant-1", "INC-ESC-1").orElseThrow();
        assertThat(updated.getLastEscalatedAt()).isNotNull();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void shouldNotEscalateWhenCooldownActive() {
        IncidentEntity incident = new IncidentEntity();
        incident.setTenantId("tenant-2");
        incident.setIncidentKey("INC-COOLDOWN");
        incident.setIncidentStatus(IncidentStatus.IN_PROGRESS);
        incident.setLastActivityAt(LocalDateTime.now().minusMinutes(30));
        incident.setLastEscalatedAt(LocalDateTime.now().minusMinutes(2)); // cooldown 5
        incidentRepository.saveAndFlush(incident);

        AlertResponse alert = new AlertResponse();
        alert.setSeverity(Severity.CRITICAL);
        alert.setScore(95);
        alert.setReason("urgent");

        when(incidentService.evaluateSeverityPublic(any())).thenAnswer(invocation -> {
            IncidentEntity arg = invocation.getArgument(0);
            return "INC-COOLDOWN".equals(arg.getIncidentKey()) ? alert : null;
        });

        incidentMonitor.monitorIncidents();

        verify(slackNotifier, never()).sendAlert(anyString());

        IncidentEntity updated = incidentRepository.findByTenantIdAndIncidentKey("tenant-2", "INC-COOLDOWN").orElseThrow();
        // lastEscalatedAt should remain the same (recent value)
        assertThat(updated.getLastEscalatedAt()).isNotNull();
    }

    @Test
    void shouldNotEscalateNonCriticalSeverity() {
        IncidentEntity incident = new IncidentEntity();
        incident.setTenantId("tenant-3");
        incident.setIncidentKey("INC-NONCRIT");
        incident.setIncidentStatus(IncidentStatus.IN_PROGRESS);
        incident.setLastActivityAt(LocalDateTime.now().minusMinutes(30));
        incidentRepository.saveAndFlush(incident);

        AlertResponse alert = new AlertResponse();
        alert.setSeverity(Severity.LOW);
        alert.setScore(10);
        alert.setReason("not important");

        when(incidentService.evaluateSeverityPublic(any())).thenAnswer(invocation -> {
            IncidentEntity arg = invocation.getArgument(0);
            return "INC-NONCRIT".equals(arg.getIncidentKey()) ? alert : null;
        });

        incidentMonitor.monitorIncidents();

        verify(slackNotifier, never()).sendAlert(anyString());

        IncidentEntity updated = incidentRepository.findByTenantIdAndIncidentKey("tenant-3", "INC-NONCRIT").orElseThrow();
        assertThat(updated.getLastEscalatedAt()).isNull();
    }

    @Test
    void shouldProcessMultipleTenants() {
        IncidentEntity a = new IncidentEntity();
        a.setTenantId("tenant-A");
        a.setIncidentKey("INC-A");
        a.setIncidentStatus(IncidentStatus.IN_PROGRESS);
        a.setLastActivityAt(LocalDateTime.now().minusMinutes(30));

        IncidentEntity b = new IncidentEntity();
        b.setTenantId("tenant-B");
        b.setIncidentKey("INC-B");
        b.setIncidentStatus(IncidentStatus.IN_PROGRESS);
        b.setLastActivityAt(LocalDateTime.now().minusMinutes(30));

        incidentRepository.saveAll(List.of(a, b));
        incidentRepository.flush();

        AlertResponse alertA = new AlertResponse();
        alertA.setSeverity(Severity.CRITICAL);
        alertA.setScore(80);
        alertA.setReason("A stale");

        AlertResponse alertB = new AlertResponse();
        alertB.setSeverity(Severity.CRITICAL);
        alertB.setScore(85);
        alertB.setReason("B stale");

        when(incidentService.evaluateSeverityPublic(any())).thenAnswer(invocation -> {
            IncidentEntity arg = invocation.getArgument(0);
            if ("INC-A".equals(arg.getIncidentKey())) return alertA;
            if ("INC-B".equals(arg.getIncidentKey())) return alertB;
            return null;
        });

        incidentMonitor.monitorIncidents();

        verify(slackNotifier, times(2)).sendAlert(anyString());

        IncidentEntity updatedA = incidentRepository.findByTenantIdAndIncidentKey("tenant-A", "INC-A").orElseThrow();
        IncidentEntity updatedB = incidentRepository.findByTenantIdAndIncidentKey("tenant-B", "INC-B").orElseThrow();

        assertThat(updatedA.getLastEscalatedAt()).isNotNull();
        assertThat(updatedB.getLastEscalatedAt()).isNotNull();
    }

}
