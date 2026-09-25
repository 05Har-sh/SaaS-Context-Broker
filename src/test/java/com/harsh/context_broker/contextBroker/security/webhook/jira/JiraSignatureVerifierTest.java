package com.harsh.context_broker.contextBroker.security.webhook.jira;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JiraSignatureVerifierTest {
    private final JiraSignatureVerifier verifier = new JiraSignatureVerifier();

    @Test
    void shouldVerifyValidSha256Signature() throws Exception {
        String rawBody = "{\"event\":\"issue_created\"}";
        String secret = "super-secret";
        String signature = "sha256=" + calculateSignature(rawBody, secret);

        assertThat(verifier.verify(rawBody, secret, signature)).isTrue();
    }

    @Test
    void shouldRejectMalformedOrMismatchedSignature() throws Exception {
        String rawBody = "{\"event\":\"issue_created\"}";
        String secret = "super-secret";

        assertThat(verifier.verify(rawBody, secret, null)).isFalse();
        assertThat(verifier.verify(rawBody, secret, "md5=abc123")).isFalse();
        assertThat(verifier.verify(rawBody, secret, "sha256=" + calculateSignature("{\"event\":\"issue_deleted\"}", secret))).isFalse();
        assertThat(verifier.verify(null, secret, "sha256=abc123")).isFalse();
        assertThat(verifier.verify(rawBody, null, "sha256=abc123")).isFalse();
    }

    private String calculateSignature(String rawBody, String secret) throws Exception {
        Mac mac = Mac.getInstance(JiraSignatureVerifier.HMAC_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), JiraSignatureVerifier.HMAC_ALGORITHM);
        mac.init(secretKey);

        byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
