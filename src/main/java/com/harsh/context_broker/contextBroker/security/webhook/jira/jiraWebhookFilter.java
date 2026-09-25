package com.harsh.context_broker.contextBroker.security.webhook.jira;

import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.security.rateLimit.RedisRateLimiter;
import com.harsh.context_broker.contextBroker.security.webhook.common.CachedBodyHttpServletRequest;
import com.harsh.context_broker.contextBroker.service.JiraWebhookEventService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class jiraWebhookFilter extends OncePerRequestFilter {
    public static final String JIRA_PATH = "/incoming/jira";
    public static final String SIGNATURE_HEADER = "X-Hub-Signature";
    private static final String WEBHOOK_ID_HEADER = "X-Atlassian-Webhook-Identifier";

    private final JiraSignatureVerifier signatureVerifier;
    private final JiraIntegrationRepository integrationRepository;
    private final ObjectMapper objectMapper;
    private final JiraWebhookEventService webhookEventService;
    private final RedisRateLimiter rateLimiter;
    private final CredentialEncryptionService encryptionService;

    public jiraWebhookFilter(JiraSignatureVerifier signatureVerifier, JiraIntegrationRepository integrationRepository, ObjectMapper objectMapper, JiraWebhookEventService webhookEventService, RedisRateLimiter rateLimiter, CredentialEncryptionService encryptionService) {
        this.signatureVerifier = signatureVerifier;
        this.integrationRepository = integrationRepository;
        this.objectMapper = objectMapper;
        this.webhookEventService = webhookEventService;
        this.rateLimiter = rateLimiter;
        this.encryptionService = encryptionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals(JIRA_PATH);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        String signature = cachedRequest.getHeader(SIGNATURE_HEADER);
        String webhookId = cachedRequest.getHeader(WEBHOOK_ID_HEADER);


        if (signature == null || signature.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Jira webhook signature");
            return;
        }
        if (webhookId == null || webhookId.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Jira webhook identifier");
            return;
        }

        JiraIntegration integration = integrationRepository.findByJiraWebhookId(webhookId).orElse(null);

        if (integration == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown Jira webhook");
            return;
        }

        String rateLimitKey =  "rate_limit:jira:integration:" + integration.getId();
        boolean allowed = rateLimiter.isAllowed(rateLimitKey);
        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        String rawBody = new String(
                cachedRequest.getCachedBody(),
                StandardCharsets.UTF_8
        );

        String webhookSecret = encryptionService.decrypt(integration.getWebhookSecret());
        boolean valid = signatureVerifier.verify(rawBody, webhookSecret, signature);
        if (!valid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Jira webhook signature");
            return;
        }

        String tenantId = integration.getTenantId();
        boolean claimed = webhookEventService.claimWebhook(tenantId, webhookId);

        if (!claimed) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        try {
            TenantContext.setTenantId(integration.getTenantId());
            filterChain.doFilter(cachedRequest, response);
        }finally {
            TenantContext.clear();
        }
    }
}
