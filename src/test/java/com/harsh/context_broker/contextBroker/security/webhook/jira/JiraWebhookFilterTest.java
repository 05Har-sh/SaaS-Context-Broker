//package com.harsh.context_broker.contextBroker.security.webhook.jira;
//
//import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
//import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
//import com.harsh.context_broker.contextBroker.service.JiraWebhookEventService;
//import com.harsh.context_broker.contextBroker.tenant.TenantContext;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.mock.web.MockHttpServletRequest;
//import org.springframework.mock.web.MockHttpServletResponse;
//import tools.jackson.databind.ObjectMapper;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//class JiraWebhookFilterTest {
//    private final JiraSignatureVerifier signatureVerifier = mock(JiraSignatureVerifier.class);
//    private final JiraIntegrationRepository integrationRepository = mock(JiraIntegrationRepository.class);
//    private final JiraWebhookEventService webhookEventService = mock(JiraWebhookEventService.class);
//    private final jiraWebhookFilter filter = new jiraWebhookFilter(
//            signatureVerifier,
//            integrationRepository,
//            new ObjectMapper(),
//            webhookEventService
//    );
//
//    @AfterEach
//    void tearDown() {
//        TenantContext.clear();
//    }
//
//    @Test
//    void shouldIgnoreRequestsForOtherPaths() {
//        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/other/path");
//
//        assertThat(filter.shouldNotFilter(request)).isTrue();
//    }
//
//    @Test
//    void shouldRejectRequestWhenSignatureHeaderIsMissing() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
//        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, "");
//        request.addHeader("X-Atlassian-Webhook-Identifier", "hook-123");
//        request.setContent("{\"issue\":\"ABC-1\"}".getBytes(StandardCharsets.UTF_8));
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        FilterChain chain = mock(FilterChain.class);
//
//        filter.doFilterInternal(request, response, chain);
//
//        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
//        assertThat(response.getErrorMessage()).isEqualTo("Missing Jira webhook signature");
//        verify(chain, never()).doFilter(any(), any());
//    }
//
//    @Test
//    void shouldRejectRequestWhenWebhookIdentifierIsMissing() throws Exception {
//        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
//        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, "sha256=abc");
//        request.setContent("{\"issue\":\"ABC-1\"}".getBytes(StandardCharsets.UTF_8));
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        FilterChain chain = mock(FilterChain.class);
//
//        filter.doFilterInternal(request, response, chain);
//
//        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
//        assertThat(response.getErrorMessage()).isEqualTo("Missing Jira webhook identifier");
//        verify(chain, never()).doFilter(any(), any());
//    }
//
//    @Test
//    void shouldRejectUnknownJiraWebhook() throws Exception {
//        MockHttpServletRequest request = createRequest("{\"issue\":\"ABC-1\"}", "hook-123", "sha256=abc");
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        FilterChain chain = mock(FilterChain.class);
//
//        when(integrationRepository.findByJiraWebhookId("hook-123")).thenReturn(Optional.empty());
//
//        filter.doFilterInternal(request, response, chain);
//
//        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
//        assertThat(response.getErrorMessage()).isEqualTo("Unknown Jira webhook");
//        verify(chain, never()).doFilter(any(), any());
//    }
//
//    @Test
//    void shouldRejectInvalidWebhookSignature() throws Exception {
//        String body = "{\"issue\":\"ABC-1\"}";
//        String webhookId = "hook-123";
//        String secret = "shared-secret";
//        JiraIntegration integration = new JiraIntegration();
//        integration.setJiraWebhookId(webhookId);
//        integration.setTenantId("tenant-42");
//        integration.setWebhookSecret(secret);
//
//        when(integrationRepository.findByJiraWebhookId(webhookId)).thenReturn(Optional.of(integration));
//        when(signatureVerifier.verify(body, secret, "sha256=abc")).thenReturn(false);
//
//        MockHttpServletRequest request = createRequest(body, webhookId, "sha256=abc");
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        FilterChain chain = mock(FilterChain.class);
//
//        filter.doFilterInternal(request, response, chain);
//
//        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
//        assertThat(response.getErrorMessage()).isEqualTo("Invalid Jira webhook signature");
//        verify(chain, never()).doFilter(any(), any());
//    }
//
//    @Test
//    void shouldAllowValidWebhookAndSetTenantContextBeforeFilterChain() throws Exception {
//        String body = "{\"issue\":\"ABC-1\"}";
//        String webhookId = "hook-123";
//        String secret = "shared-secret";
//        String signature = "sha256=" + calculateSignature(body, secret);
//        JiraIntegration integration = new JiraIntegration();
//        integration.setJiraWebhookId(webhookId);
//        integration.setTenantId("tenant-42");
//        integration.setWebhookSecret(secret);
//
//        when(integrationRepository.findByJiraWebhookId(webhookId)).thenReturn(Optional.of(integration));
//        when(signatureVerifier.verify(body, secret, signature)).thenReturn(true);
//        when(webhookEventService.claimWebhook("tenant-42", webhookId)).thenReturn(true);
//
//        MockHttpServletRequest request = createRequest(body, webhookId, signature);
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        FilterChain chain = mock(FilterChain.class);
//
//        filter.doFilterInternal(request, response, chain);
//
//        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
//        assertThat(TenantContext.getTenantId()).isNull();
//        verify(chain).doFilter(any(), eq(response));
//    }
//
//    @Test
//    void shouldSkipFilterChainWhenWebhookIsAlreadyClaimed() throws Exception {
//        String body = "{\"issue\":\"ABC-1\"}";
//        String webhookId = "hook-123";
//        String secret = "shared-secret";
//        String signature = "sha256=" + calculateSignature(body, secret);
//        JiraIntegration integration = new JiraIntegration();
//        integration.setJiraWebhookId(webhookId);
//        integration.setTenantId("tenant-42");
//        integration.setWebhookSecret(secret);
//
//        when(integrationRepository.findByJiraWebhookId(webhookId)).thenReturn(Optional.of(integration));
//        when(signatureVerifier.verify(body, secret, signature)).thenReturn(true);
//        when(webhookEventService.claimWebhook("tenant-42", webhookId)).thenReturn(false);
//
//        MockHttpServletRequest request = createRequest(body, webhookId, signature);
//        MockHttpServletResponse response = new MockHttpServletResponse();
//        FilterChain chain = mock(FilterChain.class);
//
//        filter.doFilterInternal(request, response, chain);
//
//        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
//        verify(chain, never()).doFilter(any(), any());
//    }
//
//    private MockHttpServletRequest createRequest(String body, String webhookId, String signature) {
//        MockHttpServletRequest request = new MockHttpServletRequest("POST", jiraWebhookFilter.JIRA_PATH);
//        request.addHeader(jiraWebhookFilter.SIGNATURE_HEADER, signature);
//        request.addHeader("X-Atlassian-Webhook-Identifier", webhookId);
//        request.setContent(body.getBytes(StandardCharsets.UTF_8));
//        return request;
//    }
//
//    private String calculateSignature(String rawBody, String secret) throws Exception {
//        Mac mac = Mac.getInstance(JiraSignatureVerifier.HMAC_ALGORITHM);
//        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), JiraSignatureVerifier.HMAC_ALGORITHM);
//        mac.init(secretKey);
//
//        byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
//        StringBuilder hex = new StringBuilder();
//        for (byte b : hash) {
//            hex.append(String.format("%02x", b));
//        }
//        return hex.toString();
//    }
//}
