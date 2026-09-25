package com.harsh.context_broker.contextBroker.security.webhook.jira;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * raw body + secret
 *        ↓
 * HMAC-SHA256
 *        ↓
 * compare with Jira signature
 *        ↓
 * true / false
 */

@Component
public class JiraSignatureVerifier {
    public static final String HMAC_ALGORITHM = "HmacSHA256";
    public static final String SIGNATURE_PREFIX = "sha256=";

    public boolean verify(String rawBody, String secret, String receivedSignature) {

        if(rawBody == null || secret == null || receivedSignature == null) {
            return false;
        }
        if(!receivedSignature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        try {
            String expectedSignature = calculateSugnature(rawBody, secret);

            byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
            byte[] receivedBytes = receivedSignature.getBytes(StandardCharsets.UTF_8);

            return MessageDigest.isEqual(expectedBytes, receivedBytes);
        }catch (Exception e) {
            return false;
        }
    }
    private String calculateSugnature(String rawBody, String secret) throws Exception{
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);

        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKey);

        byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));

        return SIGNATURE_PREFIX + bytesToHex(hash);
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
