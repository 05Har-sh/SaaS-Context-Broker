package com.harsh.context_broker.contextBroker.security.webhook.slack;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * RESPONSIBILITY: Verify the signature of a Slack request
 * timestamp
 * signature
 * raw request body
 * signing secret
 *         ↓
 *         HMAC-SHA256
 *         ↓
 * valid / invalid
 */

@Component
public class SlackSignatureVerifier {

    public static final long MAX_TIMESTAMP_AGE_SECONDS = 60 * 5;

    public boolean isValid(
            String timestamp,
            String signature,
            String rawBody,
            String signingSecret){

        if (timestamp == null || timestamp.isBlank()) {
            return false;
        }
        if (signature == null || signature.isBlank()) {
            return false;
        }
        if (rawBody == null) {
            return false;
        }
        if (signingSecret == null || signingSecret.isBlank()) {
            return false;
        }
        long requestTimestamp;
        try{
            requestTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long currentTimestamp = System.currentTimeMillis() / 1000;


        if (Math.abs(currentTimestamp - requestTimestamp) > MAX_TIMESTAMP_AGE_SECONDS) {
            return false;
        }
        String expectedSignature = calculateSignature(timestamp, rawBody, signingSecret);

        return MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8));
    }
    private String calculateSignature(String timestamp, String rawBody, String signingSecret) {
        try {
            String baseString = "v0:" + timestamp + ":" + rawBody;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "v0=" + hex;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate Slack signature", e);
        }
    }
}
