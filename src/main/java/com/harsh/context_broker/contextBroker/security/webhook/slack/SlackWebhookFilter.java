package com.harsh.context_broker.contextBroker.security.webhook.slack;

import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
import com.harsh.context_broker.contextBroker.security.rateLimit.RedisRateLimiter;
import com.harsh.context_broker.contextBroker.security.webhook.common.CachedBodyHttpServletRequest;
import com.harsh.context_broker.contextBroker.dto.SlackIncomingRequest;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.service.SlackIntegrationService;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SlackWebhookFilter implements Filter {
    private final SlackSignatureVerifier signatureVerifier;
    private final SlackIntegrationService integrationService;
    private final ObjectMapper objectMapper;
    private final RedisRateLimiter rateLimiter;
    private final CredentialEncryptionService credentialEncryptionService;

    public SlackWebhookFilter(SlackSignatureVerifier signatureVerifier, SlackIntegrationService integrationService, ObjectMapper objectMapper, RedisRateLimiter rateLimiter, CredentialEncryptionService credentialEncryptionService) {
        this.signatureVerifier = signatureVerifier;
        this.integrationService = integrationService;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!httpRequest.getRequestURI().equals("/incoming/slack")) {
            chain.doFilter(request, response);
            return;
        }
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);

        String rawBody = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);

        String timeStamp = cachedRequest.getHeader( "X-Slack-Request-Timestamp");

        String signature = cachedRequest.getHeader("X-Slack-Signature");

        SlackIncomingRequest slackRequest = objectMapper.readValue(rawBody, SlackIncomingRequest.class);

        String teamId = slackRequest.getTeamId();

        SlackIntegration integration = integrationService.getByTeamId(teamId);

        String rateLimitKey = "rate_limit:slack:integration:" + integration.getId();

        boolean allowed = rateLimiter.isAllowed(rateLimitKey);

        if (!allowed) {
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        String signingSecret = credentialEncryptionService.decrypt(integration.getSigningSecret());
        boolean valid = signatureVerifier.isValid(
                timeStamp,
                signature,
                rawBody,
                signingSecret
        );
        if (!valid) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String tenantId = integration.getTenantId();
        TenantContext.setTenantId(tenantId);

        try {
            chain.doFilter(cachedRequest, response);
        } finally {
            TenantContext.clear();
        }
    }
}
