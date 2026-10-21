package com.harsh.context_broker.contextBroker.oauth.slackstate;

import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OAuthStateService {
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private static final String KEY_PREFIX = "oauth:slack:state:";

    private final StringRedisTemplate redisTemplate;

    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthStateService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createState(String tenantId, String userId) {
        if (tenantId == null || tenantId.isBlank()
                || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant ID and user ID are required for OAuth state"
            );
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String key = KEY_PREFIX + state;
        long createdAt = System.currentTimeMillis();
        long expiresAt = createdAt + STATE_TTL.toMillis();

        List<Object> results = redisTemplate.execute(new SessionCallback<>() {
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                operations.opsForHash().put(key, "tenantId", tenantId);
                operations.opsForHash().put(key, "userId", userId);
                operations.opsForHash().put(key, "createdAt", Long.toString(createdAt));
                operations.opsForHash().put(key, "expiresAt", Long.toString(expiresAt));
                operations.expire(key, STATE_TTL);
                return operations.exec();
            }
        });

        if (results == null) {
            throw new IllegalStateException("Failed to persist Slack OAuth state");
        }

        return state;
    }

    public OAuthStateData consumeState(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("OAuth state is missing");
        }

        String key = KEY_PREFIX + state;
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);

        if (values.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }

        String tenantId = (String) values.get("tenantId");
        String userId = (String) values.get("userId");
        String expiresAtValue = (String) values.get("expiresAt");

        if (tenantId == null || userId == null) {
            redisTemplate.delete(key);
            throw new IllegalArgumentException("Invalid OAuth state data");
        }

        if (expiresAtValue != null) {
            try {
                long expiresAt = Long.parseLong(expiresAtValue);
                if (System.currentTimeMillis() > expiresAt) {
                    redisTemplate.delete(key);
                    throw new IllegalArgumentException("Expired OAuth state");
                }
            } catch (NumberFormatException ex) {
                redisTemplate.delete(key);
                throw new IllegalArgumentException("Invalid OAuth state expiry");
            }
        }

        redisTemplate.delete(key);
        return new OAuthStateData(tenantId, userId);
    }
}
