package com.harsh.context_broker.contextBroker.oauth.jirastate;

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
public class JiraOAuthStateService {
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    private static final String KEY_PREFIX = "oauth:jira:state:";

    private final StringRedisTemplate redisTemplate;

    private SecureRandom secureRandom = new SecureRandom();

    public JiraOAuthStateService(StringRedisTemplate redisTemplate) {
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

        List<Object> results = redisTemplate.execute(
                new SessionCallback<>() {
                    @Override
                    public List<Object> execute(
                            RedisOperations operations) {
                        operations.multi();
                        operations.opsForHash().put(
                                key, "tenantId", tenantId);
                        operations.opsForHash().put(
                                key, "userId", userId);
                        operations.expire(key, STATE_TTL);
                        return operations.exec();
                    }
                }
        );

        if (results == null) {
            throw new IllegalStateException(
                    "Failed to persist Jira OAuth state"
            );
        }

        return state;
    }

    public OAuthStateData consumeState(String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("OAuth state is missing");
        }
        String key = KEY_PREFIX + state;

        Boolean exists = redisTemplate.hasKey(key);
        System.out.println(
                "Jira OAuth state exists in Redis: " + exists
        );

        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);

        if (values.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired OAuth state");
        }
        String tenantId = (String) values.get("tenantId");
        String userId = (String) values.get("userId");

        if (tenantId == null || userId == null) {
            redisTemplate.delete(key);
            throw new IllegalArgumentException("Invalid OAuth state data");
        }
        redisTemplate.delete(key);
        return new OAuthStateData(
                tenantId,
                userId
        );
    }
}
