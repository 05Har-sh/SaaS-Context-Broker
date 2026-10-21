package com.harsh.context_broker.contextBroker;

import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
import com.harsh.context_broker.contextBroker.oauth.slackstate.OAuthStateService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackOAuthStateServiceTest {

    @Test
    void stateIsGeneratedAndConsumedOnce() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.execute(any(SessionCallback.class))).thenReturn(List.of());

        OAuthStateService service = new OAuthStateService(redisTemplate);
        String state = service.createState("tenant-A", "user-123");

        assertThat(state).isNotBlank();
        assertThat(state).matches("[A-Za-z0-9_-]+")
                .hasSizeGreaterThanOrEqualTo(20);

        verify(redisTemplate).execute(any(SessionCallback.class));

        when(hashOperations.entries(anyString())).thenReturn(Map.of(
                "tenantId", "tenant-A",
                "userId", "user-123",
                "expiresAt", String.valueOf(System.currentTimeMillis() + 60000)
        ));

        OAuthStateData data = service.consumeState(state);
        assertThat(data.tenantId()).isEqualTo("tenant-A");
        assertThat(data.userId()).isEqualTo("user-123");

        when(hashOperations.entries(anyString())).thenReturn(Map.of());
        assertThatThrownBy(() -> service.consumeState(state))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");
    }
}
