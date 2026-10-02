package com.harsh.context_broker.contextBroker.security.webhook.jira;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.security.rateLimit.RedisRateLimiter;
import com.harsh.context_broker.contextBroker.security.webhook.jira.oauth.AtlassianWebhookJwtVerifier;
import com.harsh.context_broker.contextBroker.service.JiraWebhookEventService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JiraWebhookFilterTest {
    private final JiraSignatureVerifier signatureVerifier = mock(JiraSignatureVerifier.class);
    private final JiraIntegrationRepository integrationRepository = mock(JiraIntegrationRepository.class);
    private final JiraWebhookEventService webhookEventService = mock(JiraWebhookEventService.class);
    private final RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
    private final CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
    private final AtlassianWebhookJwtVerifier oauthWebhookVerifier = mock(AtlassianWebhookJwtVerifier.class);
    private final jiraWebhookFilter filter = new jiraWebhookFilter(
            signatureVerifier,
            integrationRepository,
            new ObjectMapper(),
            webhookEventService,
            rateLimiter,
            encryptionService,
            oauthWebhookVerifier
    );

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldIgnoreRequestsForOtherPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/other/path");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldProcessOnlyIncomingJiraPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void shouldRejectRequestWhenWebhookIdentifierIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Missing Jira webhook identifier");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectUnknownJiraWebhook() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader("Authorization", "Bearer abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Unknown Jira webhook");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectWhenRateLimitIsExceeded() throws Exception {
        JiraIntegration integration = new JiraIntegration();
        integration.setId(42L);
        integration.setTenantId("tenant-42");
        integration.setJiraWebhookId("hook-123");
        integration.setIntegrationType(JiraIntegrationType.MANUAL);
        integration.setWebhookSecret("encrypted-secret");

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, "sha256=abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectMissingSignatureForManualWebhook() throws Exception {
        JiraIntegration integration = integrationForManualWebhook();
        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.setContent("{\"issue\":\"ABC-1\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Missing Jira webhook signature");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectInvalidManualWebhookAuthentication() throws Exception {
        String body = "{\"issue\":\"ABC-1\"}";
        String signature = "sha256=abc";
        JiraIntegration integration = integrationForManualWebhook();

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(true);
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("shared-secret");
        when(signatureVerifier.verify(body, "shared-secret", signature)).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, signature);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Invalid Jira webhook authentication");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAllowValidManualWebhookAndSetTenantContextBeforeChain() throws Exception {
        String body = "{\"issue\":\"ABC-1\"}";
        String signature = "sha256=abc";
        JiraIntegration integration = integrationForManualWebhook();

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(true);
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("shared-secret");
        when(signatureVerifier.verify(body, "shared-secret", signature)).thenReturn(true);
        when(webhookEventService.claimWebhook("tenant-42", "hook-123")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, signature);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(chain).doFilter(any(), eq(response));
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void shouldSkipFilterChainWhenWebhookAlreadyClaimed() throws Exception {
        String body = "{\"issue\":\"ABC-1\"}";
        String signature = "sha256=abc";
        JiraIntegration integration = integrationForManualWebhook();

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(true);
        when(encryptionService.decrypt("encrypted-secret")).thenReturn("shared-secret");
        when(signatureVerifier.verify(body, "shared-secret", signature)).thenReturn(true);
        when(webhookEventService.claimWebhook("tenant-42", "hook-123")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, signature);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAllowValidOauthWebhook() throws Exception {
        JiraIntegration integration = new JiraIntegration();
        integration.setId(42L);
        integration.setTenantId("tenant-42");
        integration.setJiraWebhookId("hook-123");
        integration.setIntegrationType(JiraIntegrationType.OAUTH);

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(true);
        when(oauthWebhookVerifier.verify("Bearer access-token")).thenReturn(true);
        when(webhookEventService.claimWebhook("tenant-42", "hook-123")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader("Authorization", "Bearer access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        verify(chain).doFilter(any(), eq(response));
    }

    @Test
    void shouldRejectInvalidOauthWebhookAuthentication() throws Exception {
        JiraIntegration integration = new JiraIntegration();
        integration.setId(42L);
        integration.setTenantId("tenant-42");
        integration.setJiraWebhookId("hook-123");
        integration.setIntegrationType(JiraIntegrationType.OAUTH);

        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.of(integration));
        when(rateLimiter.isAllowed("rate_limit:jira:integration:42")).thenReturn(true);
        when(oauthWebhookVerifier.verify("Bearer invalid-token")).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getErrorMessage()).isEqualTo("Invalid Jira webhook authentication");
        verify(chain, never()).doFilter(any(), any());
    }

    private JiraIntegration integrationForManualWebhook() {
        JiraIntegration integration = new JiraIntegration();
        integration.setId(42L);
        integration.setTenantId("tenant-42");
        integration.setJiraWebhookId("hook-123");
        integration.setIntegrationType(JiraIntegrationType.MANUAL);
        integration.setWebhookSecret("encrypted-secret");
        return integration;
    }
}
