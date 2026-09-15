package com.harsh.context_broker.contextBroker;

import com.harsh.context_broker.contextBroker.dto.IncidentResponse;
import com.harsh.context_broker.contextBroker.dto.TimelineEventResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import com.harsh.context_broker.contextBroker.entity.IncidentEventEntity;
import com.harsh.context_broker.contextBroker.entity.TimelineEventEntity;
import com.harsh.context_broker.contextBroker.exception.ResourceNotFoundException;
import com.harsh.context_broker.contextBroker.model.IncidentStatus;
import com.harsh.context_broker.contextBroker.repository.IncidentEventRepository;
import com.harsh.context_broker.contextBroker.repository.IncidentRepository;
import com.harsh.context_broker.contextBroker.repository.TimelineEventRepository;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import com.harsh.context_broker.contextBroker.service.TimelineService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import com.harsh.context_broker.contextBroker.tenant.TenantFilter;
import jakarta.servlet.FilterChain;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TenantIsolationIntegrationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentEventRepository incidentEventRepository;

    @Autowired
    private TimelineEventRepository timelineEventRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private TimelineService timelineService;

    @Autowired
    private TenantFilter tenantFilter;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        incidentEventRepository.deleteAll();
        timelineEventRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    @Test
    void shouldNotAllowTenantBToAccessTenantAIncident() {
        IncidentEntity tenantAIncident = createIncident("tenant-A", "INC-001");
        incidentRepository.saveAndFlush(tenantAIncident);

        TenantContext.setTenantId("tenant-B");

        assertThatThrownBy(() -> incidentService.getIncidentByKey("INC-001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident not found: INC-001");

        TenantContext.setTenantId("tenant-A");
        IncidentEntity retrieved = incidentService.getIncidentByKey("INC-001");
        assertThat(retrieved.getTenantId()).isEqualTo("tenant-A");
        assertThat(retrieved.getIncidentKey()).isEqualTo("INC-001");
    }

    @Test
    void shouldReturnOnlyIncidentsBelongingToCurrentTenant() {
        incidentRepository.saveAllAndFlush(List.of(
                createIncident("tenant-A", "INC-001"),
                createIncident("tenant-A", "INC-002"),
                createIncident("tenant-B", "INC-003")
        ));

        TenantContext.setTenantId("tenant-A");
        List<String> tenantAKeys = incidentService.getAllIncidents(0, 20, null, null, null, null)
                .getContent()
                .stream()
                .map(IncidentResponse::getIncidentKey)
                .toList();

        assertThat(tenantAKeys).containsExactlyInAnyOrder("INC-001", "INC-002");
        assertThat(tenantAKeys).doesNotContain("INC-003");

        TenantContext.setTenantId("tenant-B");
        List<String> tenantBKeys = incidentService.getAllIncidents(0, 20, null, null, null, null)
                .getContent()
                .stream()
                .map(IncidentResponse::getIncidentKey)
                .toList();

        assertThat(tenantBKeys).containsExactly("INC-003");
    }

    @Test
    void shouldReturnOnlyTimelineEventsForCurrentTenant() {
        createTimelineEvent("tenant-A", "INC-001", "tenant-a-event-1");
        createTimelineEvent("tenant-B", "INC-001", "tenant-b-event-1");

        TenantContext.setTenantId("tenant-A");
        List<TimelineEventResponse> tenantAEvents = timelineService.getTimelineForIncident("INC-001");
        assertThat(tenantAEvents)
                .extracting(TimelineEventResponse::getContent)
                .containsExactly("tenant-a-event-1");

        TenantContext.setTenantId("tenant-B");
        List<TimelineEventResponse> tenantBEvents = timelineService.getTimelineForIncident("INC-001");
        assertThat(tenantBEvents)
                .extracting(TimelineEventResponse::getContent)
                .containsExactly("tenant-b-event-1");
    }

    @Test
    void shouldReturnOnlyIncidentEventsForCurrentTenant() {
        createIncidentEvent("tenant-A", "INC-001", "tenant-a-legacy");
        createIncidentEvent("tenant-B", "INC-001", "tenant-b-legacy");

        List<IncidentEventEntity> tenantAEvents = incidentEventRepository
                .findByTenantIdAndIncidentKeyOrderByTimestampAsc("tenant-A", "INC-001");

        assertThat(tenantAEvents)
                .extracting(IncidentEventEntity::getContent)
                .containsExactly("tenant-a-legacy");

        List<IncidentEventEntity> tenantBEvents = incidentEventRepository
                .findByTenantIdAndIncidentKeyOrderByTimestampAsc("tenant-B", "INC-001");

        assertThat(tenantBEvents)
                .extracting(IncidentEventEntity::getContent)
                .containsExactly("tenant-b-legacy");
    }

    @Test
    void shouldAllowSameIncidentKeyForDifferentTenants() {
        IncidentEntity tenantAIncident = createIncident("tenant-A", "INC-001");
        IncidentEntity tenantBIncident = createIncident("tenant-B", "INC-001");
        incidentRepository.saveAllAndFlush(List.of(tenantAIncident, tenantBIncident));

        TenantContext.setTenantId("tenant-A");
        IncidentEntity aLookup = incidentService.getIncidentByKey("INC-001");
        assertThat(aLookup.getTenantId()).isEqualTo("tenant-A");

        TenantContext.setTenantId("tenant-B");
        IncidentEntity bLookup = incidentService.getIncidentByKey("INC-001");
        assertThat(bLookup.getTenantId()).isEqualTo("tenant-B");

        assertThat(incidentRepository.findAllByTenantId("tenant-A")).singleElement().satisfies(i -> {
            assertThat(i.getIncidentKey()).isEqualTo("INC-001");
        });
        assertThat(incidentRepository.findAllByTenantId("tenant-B")).singleElement().satisfies(i -> {
            assertThat(i.getIncidentKey()).isEqualTo("INC-001");
        });
    }

    @Test
    void shouldRejectDuplicateIncidentKeyWithinSameTenant() {
        incidentRepository.saveAndFlush(createIncident("tenant-A", "INC-001"));

        IncidentEntity duplicate = createIncident("tenant-A", "INC-001");

        assertThatThrownBy(() -> incidentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldClearTenantContextAfterRequest() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("tenant_id", "tenant-A")
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-A");
            throw new RuntimeException("forced exception");
        };

        assertThatThrownBy(() -> tenantFilter.doFilter(request, response, chain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("forced exception");

        assertThat(TenantContext.getTenantId()).isNull();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectTenantOwnedOperationWhenTenantContextIsMissing() {
        TenantContext.clear();

        assertThatThrownBy(() -> incidentService.getIncidentByKey("INC-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No tenant context available");
    }

    private IncidentEntity createIncident(String tenantId, String incidentKey) {
        IncidentEntity incident = new IncidentEntity();
        incident.setTenantId(tenantId);
        incident.setIncidentKey(incidentKey);
        incident.setLastMsg("test message");
        incident.setPostedAt(LocalDateTime.now());
        incident.setLastUpdated(LocalDateTime.now());
        incident.setLastActivityAt(LocalDateTime.now());
        incident.setIncidentStatus(IncidentStatus.CREATED);
        return incident;
    }

    private void createTimelineEvent(String tenantId, String incidentKey, String content) {
        TimelineEventEntity event = new TimelineEventEntity();
        event.setTenantId(tenantId);
        event.setIncidentKey(incidentKey);
        event.setEventType("MESSAGE_RECEIVED");
        event.setDescription("Test message");
        event.setSource("SLACK");
        event.setContent(content);
        event.setTimestamp(LocalDateTime.now());
        timelineEventRepository.save(event);
    }

    private void createIncidentEvent(String tenantId, String incidentKey, String content) {
        IncidentEventEntity event = new IncidentEventEntity();
        event.setTenantId(tenantId);
        event.setIncidentKey(incidentKey);
        event.setSource("SLACK");
        event.setContent(content);
        event.setTimestamp(LocalDateTime.now());
        incidentEventRepository.save(event);
    }
}
