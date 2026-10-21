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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class SlackWebhookFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(SlackWebhookFilter.class);

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
    @Value("${slack.oauth.signing-secret}")
    private String appSigningSecret;

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().equals("/incoming/slack")) {
            chain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(httpRequest);

        String rawBody = new String(
                cachedRequest.getCachedBody(),
                StandardCharsets.UTF_8
        );

        String timestamp =
                cachedRequest.getHeader("X-Slack-Request-Timestamp");

        String signature =
                cachedRequest.getHeader("X-Slack-Signature");

        boolean hasTimestampHeader = timestamp != null && !timestamp.isBlank();
        boolean hasSignatureHeader = signature != null && !signature.isBlank();

        JsonNode payload = objectMapper.readTree(rawBody);
        String type = payload.path("type").asText();

        // 1. Handle Slack URL verification.
        if ("url_verification".equals(type)) {

            boolean valid = signatureVerifier.isValid(
                    timestamp,
                    signature,
                    rawBody,
                    appSigningSecret
            );

            if (!valid) {
                String reason = hasTimestampHeader ? "SIGNATURE_INVALID" : "TIMESTAMP_INVALID";
                log.warn("Slack webhook rejected: reason={}, integrationLookupSucceeded=false, hasTimestampHeader={}, hasSignatureHeader={}", reason, hasTimestampHeader, hasSignatureHeader);
                httpResponse.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );
                return;
            }

            String challenge = payload.path("challenge").asText();

            if (challenge.isBlank()) {
                httpResponse.setStatus(
                        HttpServletResponse.SC_BAD_REQUEST
                );
                return;
            }

            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.setContentType("application/json");
            httpResponse.setCharacterEncoding("UTF-8");

            objectMapper.writeValue(
                    httpResponse.getWriter(),
                    Map.of("challenge", challenge)
            );

            return;
        }

        // 2. Normal events: identify the candidate workspace.
        String teamId = payload.path("team_id").asText();

        if (teamId.isBlank()) {
            teamId = payload.path("teamId").asText();
        }

        if (teamId.isBlank()) {
            httpResponse.setStatus(
                    HttpServletResponse.SC_BAD_REQUEST
            );
            return;
        }

        final SlackIntegration integration;
        boolean integrationLookupSucceeded = false;

        try {
            integration = integrationService.getByTeamId(teamId);
            integrationLookupSucceeded = true;
        } catch (IllegalArgumentException ex) {
            log.warn("Slack webhook rejected: reason=INTEGRATION_NOT_FOUND, integrationLookupSucceeded={}, hasTimestampHeader={}, hasSignatureHeader={}", integrationLookupSucceeded, hasTimestampHeader, hasSignatureHeader);
            httpResponse.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
            return;
        }

        if (!integration.isActive()) {
            log.warn("Slack webhook rejected: reason=INTEGRATION_DISABLED, teamId={}, integrationLookupSucceeded=true", teamId);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 3. Rate-limit the candidate integration.
        String rateLimitKey =
                "rate_limit:slack:integration:" + integration.getId();

        if (!rateLimiter.isAllowed(rateLimitKey)) {
            httpResponse.setStatus(
                    HttpStatus.TOO_MANY_REQUESTS.value()
            );
            return;
        }

        // 4. Verify the normal event using its stored secret.
        String signingSecret = credentialEncryptionService.decrypt(
                integration.getSigningSecret()
        );

        boolean valid = signatureVerifier.isValid(
                timestamp,
                signature,
                rawBody,
                signingSecret
        );

        if (!valid) {
            String reason = hasTimestampHeader ? "SIGNATURE_INVALID" : "TIMESTAMP_INVALID";
            log.warn("Slack webhook rejected: reason={}, integrationLookupSucceeded=true, hasTimestampHeader={}, hasSignatureHeader={}", reason, hasTimestampHeader, hasSignatureHeader);
            httpResponse.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );
            return;
        }

        // 5. Establish tenant context only after verification.
        TenantContext.setTenantId(integration.getTenantId());

        try {
            chain.doFilter(cachedRequest, httpResponse);
        } finally {
            TenantContext.clear();
        }
    }
}
