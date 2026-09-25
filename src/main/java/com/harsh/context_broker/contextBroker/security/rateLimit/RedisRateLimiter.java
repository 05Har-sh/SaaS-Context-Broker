package com.harsh.context_broker.contextBroker.security.rateLimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final DefaultRedisScript<Long> tokenBucketScript;

    public RedisRateLimiter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;

        this.tokenBucketScript = new DefaultRedisScript<>();
        try {
            ClassPathResource resource = new ClassPathResource("scripts/token_bucket.lua");

            String script = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            this.tokenBucketScript.setScriptText(script);
            this.tokenBucketScript.setResultType(Long.class);
        }catch (Exception e) {
            throw new IllegalStateException( "Failed to load token bucket Lua script", e);
        }
    }
    public boolean isAllowed(String key) {
        Long result = redisTemplate.execute(
                tokenBucketScript,
                List.of(key),
                String.valueOf(properties.getCapacity()),
                String.valueOf(properties.getRefillRate()),
                String.valueOf(Instant.now().toEpochMilli() / 1000.0),
                String.valueOf(properties.getTtlSeconds())
        );
        return result != null && result == 1L;
    }
}
