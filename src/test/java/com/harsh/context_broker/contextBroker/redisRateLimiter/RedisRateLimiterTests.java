package com.harsh.context_broker.contextBroker.redisRateLimiter;

import com.harsh.context_broker.contextBroker.security.rateLimit.RedisRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisRateLimiterTests {

    private final RedisRateLimiter rateLimiter;

    @Autowired
    public RedisRateLimiterTests(RedisRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/test-rate-limit")
    public String testRateLimit() {
        boolean allowed = rateLimiter.isAllowed("rate_limit:test:user-1");
        return allowed ? "ALLOWED" : "RATE LIMITED";
    }
}
