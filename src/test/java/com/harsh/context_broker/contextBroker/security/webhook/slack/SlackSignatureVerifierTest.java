package com.harsh.context_broker.contextBroker.security.webhook.slack;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlackSignatureVerifierTest {

    private final SlackSignatureVerifier verifier = new SlackSignatureVerifier();

    @Test
    void validSignatureIsAccepted() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String secret = "test-secret";
        String rawBody = "{\"type\":\"event_callback\",\"team_id\":\"T123\",\"event\":{\"type\":\"message\",\"text\":\"incidentKey:INC565, message:TestAlertttt\"}}";

        String signature = calculateSignature(timestamp, rawBody, secret);

        assertThat(verifier.isValid(timestamp, signature, rawBody, secret)).isTrue();
    }

    @Test
    void modifiedBodyIsRejected() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String secret = "test-secret";
        String rawBody = "{\"type\":\"event_callback\",\"team_id\":\"T123\",\"event\":{\"type\":\"message\",\"text\":\"incidentKey:INC565, message:TestAlertttt\"}}";
        String alteredBody = rawBody.replace("TestAlertttt", "Changed");

        String signature = calculateSignature(timestamp, rawBody, secret);

        assertThat(verifier.isValid(timestamp, signature, alteredBody, secret)).isFalse();
    }

    @Test
    void wrongSecretIsRejected() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String secret = "test-secret";
        String wrongSecret = "wrong-secret";
        String rawBody = "{\"type\":\"event_callback\",\"team_id\":\"T123\",\"event\":{\"type\":\"message\",\"text\":\"incidentKey:INC565, message:TestAlertttt\"}}";

        String signature = calculateSignature(timestamp, rawBody, secret);

        assertThat(verifier.isValid(timestamp, signature, rawBody, wrongSecret)).isFalse();
    }

    @Test
    void staleTimestampIsRejected() {
        String secret = "test-secret";
        String rawBody = "{\"type\":\"event_callback\",\"team_id\":\"T123\"}";
        String staleTimestamp = String.valueOf(System.currentTimeMillis() / 1000L - 600);

        String signature = calculateSignature(staleTimestamp, rawBody, secret);

        assertThat(verifier.isValid(staleTimestamp, signature, rawBody, secret)).isFalse();
    }

    private static String calculateSignature(String timestamp, String rawBody, String signingSecret) {
        String base = "v0:" + timestamp + ":" + rawBody;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec key = new javax.crypto.spec.SecretKeySpec(
                    signingSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(key);
            byte[] hash = mac.doFinal(base.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "v0=" + hex;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
