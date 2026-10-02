package com.harsh.context_broker.contextBroker.oauth.slackstate;

import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
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
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String key = KEY_PREFIX + state;

        redisTemplate.opsForHash().put(key, "tenantId", tenantId);
        redisTemplate.opsForHash().put(key, "userId", userId);

        redisTemplate.expire(key, STATE_TTL);
        return state;
    }

    public OAuthStateData consumeState(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("OAuth state is missing");
        }
        String key = KEY_PREFIX + state;
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("invalid or expired OAuth state");
        }
        String tenantId = (String) values.get("tenantId");
        String userId = (String) values.get("userId");
        if (tenantId == null || userId == null) {
            redisTemplate.delete(key);
            throw new IllegalArgumentException("invalid OAuth state");
        }
        //single use state, so delete it after consumption
        redisTemplate.delete(key);
        return new OAuthStateData(tenantId, userId);
    }
}
