package com.harsh.context_broker.contextBroker;

import com.harsh.context_broker.contextBroker.controller.SlackController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.dto.UpdateResponse;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.model.Severity;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.security.rateLimit.RedisRateLimiter;
import com.harsh.context_broker.contextBroker.security.webhook.slack.SlackSignatureVerifier;
import com.harsh.context_broker.contextBroker.security.webhook.slack.SlackWebhookFilter;
import com.harsh.context_broker.contextBroker.service.IncidentService;
import com.harsh.context_broker.contextBroker.service.SlackIntegrationService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackControllerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void validEventCallbackParsesIncidentKeyAndMessage() throws Exception {
        IncidentService incidentService = mock(IncidentService.class);
        AlertResponse alert = new AlertResponse();
        alert.setScore(12);
        alert.setReason("ok");
        alert.setSeverity(Severity.LOW);
        when(incidentService.handleUpdate("INC565", "TestAlertttt")).thenReturn(alert);

        SlackController controller = new SlackController(incidentService);
        JsonNode request = mapper.readTree("""
                {
                  "type": "event_callback",
                  "team_id": "T0AFRJ2RD63",
                  "event": {
                    "type": "message",
                    "text": "teamId:t1234, incidentKey:INC565, message:TestAlertttt",
                    "bot_id": null
                  }
                }
                """);

        ResponseEntity<?> response = controller.receiveSlack(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isInstanceOf(UpdateResponse.class);
        UpdateResponse body = (UpdateResponse) response.getBody();
        assertThat(body.getIncidentKey()).isEqualTo("INC565");
        assertThat(body.getAlert()).isSameAs(alert);
        verify(incidentService).handleUpdate("INC565", "TestAlertttt");
    }

    @Test
    void unsupportedEventsAreIgnoredSafely() throws Exception {
        IncidentService incidentService = mock(IncidentService.class);
        SlackController controller = new SlackController(incidentService);

        JsonNode request = mapper.readTree("""
                {
                  "type": "event_callback",
                  "event": {
                    "type": "reaction_added",
                    "user": "U123"
                  }
                }
                """);

        ResponseEntity<?> response = controller.receiveSlack(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
    }

    @Test
    void malformedMessageTextIsIgnored() throws Exception {
        IncidentService incidentService = mock(IncidentService.class);
        SlackController controller = new SlackController(incidentService);

        JsonNode request = mapper.readTree("""
                {
                  "type": "event_callback",
                  "event": {
                    "type": "message",
                    "text": "hello from slack"
                  }
                }
                """);

        ResponseEntity<?> response = controller.receiveSlack(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNull();
    }

    @Test
    void urlVerificationChallengeIsPreserved() throws Exception {
        IncidentService incidentService = mock(IncidentService.class);
        SlackController controller = new SlackController(incidentService);

        JsonNode request = mapper.readTree("""
                {
                  "type": "url_verification",
                  "challenge": "abc123"
                }
                """);

        ResponseEntity<?> response = controller.receiveSlack(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("abc123");
    }

    @Test
    void filterSetsTenantContextOnlyAfterSignatureVerification() throws Exception {
        SlackSignatureVerifier signatureVerifier = mock(SlackSignatureVerifier.class);
        SlackIntegrationService integrationService = mock(SlackIntegrationService.class);
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        CredentialEncryptionService credentialEncryptionService = mock(CredentialEncryptionService.class);
        tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();

        SlackIntegration integration = new SlackIntegration();
        integration.setId(99L);
        integration.setTenantId("tenant-42");
        integration.setTeamId("T123");
        integration.setSigningSecret("encrypted-secret");

        String rawBody = "{\"type\":\"event_callback\",\"team_id\":\"T123\",\"event\":{\"type\":\"message\",\"text\":\"incidentKey:INC565, message:TestAlertttt\"}}";

        when(integrationService.getByTeamId("T123")).thenReturn(integration);
        when(rateLimiter.isAllowed("rate_limit:slack:integration:99")).thenReturn(true);
        when(credentialEncryptionService.decrypt("encrypted-secret")).thenReturn("actual-secret");
        when(signatureVerifier.isValid("1700000000", "v0=signature", rawBody, "actual-secret")).thenReturn(true);

        SlackWebhookFilter filter = new SlackWebhookFilter(
                signatureVerifier,
                integrationService,
                objectMapper,
                rateLimiter,
                credentialEncryptionService
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/incoming/slack");
        request.addHeader("X-Slack-Request-Timestamp", "1700000000");
        request.addHeader("X-Slack-Signature", "v0=signature");
        request.setContent(rawBody.getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (servletRequest, servletResponse) -> {
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-42");
        };

        filter.doFilter(request, response, chain);

        assertThat(TenantContext.getTenantId()).isNull();
        verify(signatureVerifier).isValid("1700000000", "v0=signature", rawBody, "actual-secret");
    }
}
