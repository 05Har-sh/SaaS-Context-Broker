package com.harsh.context_broker.contextBroker.security.webhook.jira.oauth;

import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;

@Component
public class AtlassianWebhookJwtVerifier {

    private final JwtDecoder jwtDecoder;

    public AtlassianWebhookJwtVerifier(
            JiraOAuthProperties properties
    ) {
        if (properties.getClientSecret() == null
                || properties.getClientSecret().isBlank()) {
            throw new IllegalArgumentException(
                    "Jira OAuth client secret is missing"
            );
        }

        SecretKeySpec secretKey = new SecretKeySpec(
                properties.getClientSecret()
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        this.jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public boolean verify(String authorizationHeader) {

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {
            return false;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            return false;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);

            return jwt != null;

        } catch (Exception e) {
            return false;
        }
    }
}