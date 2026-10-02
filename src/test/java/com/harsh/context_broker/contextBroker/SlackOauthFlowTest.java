//package com.harsh.context_broker.contextBroker;
//
//import com.harsh.context_broker.contextBroker.client.SlackOAuthClient;
//import com.harsh.context_broker.contextBroker.config.slackoauth.SlackOAuthProperties;
//import com.harsh.context_broker.contextBroker.dto.oauth.SlackOAuthAccessResponse;
//import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
//import com.harsh.context_broker.contextBroker.oauth.OAuthStateData;
//import com.harsh.context_broker.contextBroker.oauth.slackstate.OAuthStateService;
//import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
//import com.harsh.context_broker.contextBroker.security.credential.CredentialEncryptionService;
//import com.harsh.context_broker.contextBroker.service.SlackIntegrationService;
//import com.harsh.context_broker.contextBroker.service.oauth.SlackOAuthService;
//import com.harsh.context_broker.contextBroker.tenant.TenantContext;
//import com.harsh.context_broker.contextBroker.tenant.TenantException;
//import com.harsh.context_broker.contextBroker.tenant.TenantResolver;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.data.redis.core.HashOperations;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.oauth2.jwt.Jwt;
//
//import java.time.Duration;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.argThat;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoInteractions;
//import static org.mockito.Mockito.when;
//
//class SlackOauthFlowTest {
//
//    @AfterEach
//    void tearDown() {
//        TenantContext.clear();
//    }
//
//    @Test
//    void shouldResolveTenantIdFromJwtClaim() {
//        TenantResolver resolver = new TenantResolver();
//
//        Jwt jwt = Jwt.withTokenValue("token")
//                .header("alg", "none")
//                .claim("tenant_id", "tenant-A")
//                .build();
//
//        assertThat(resolver.resolveTenant(jwt)).isEqualTo("tenant-A");
//
//        assertThatThrownBy(() -> resolver.resolveTenant(null))
//                .isInstanceOf(TenantException.class)
//                .hasMessageContaining("Authenticated JWT is missing");
//
//        Jwt missingTenantJwt = Jwt.withTokenValue("token")
//                .header("alg", "none")
//                .claim("sub", "user-1")
//                .build();
//
//        assertThatThrownBy(() -> resolver.resolveTenant(missingTenantJwt))
//                .isInstanceOf(TenantException.class)
//                .hasMessageContaining("Tenant ID is missing");
//    }
//
//    @Test
//    void shouldRequireTenantContextForSlackOauthFlow() {
//        assertThatThrownBy(TenantContext::requireTenantId)
//                .isInstanceOf(IllegalStateException.class)
//                .hasMessageContaining("No tenant context available");
//
//        TenantContext.setTenantId("tenant-A");
//        assertThat(TenantContext.getTenantId()).isEqualTo("tenant-A");
//        assertThat(TenantContext.requireTenantId()).isEqualTo("tenant-A");
//
//        TenantContext.clear();
//        assertThat(TenantContext.getTenantId()).isNull();
//    }
//
//    @Test
//    void shouldPersistSlackOauthStateInRedis() {
//        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
//        @SuppressWarnings("unchecked")
//        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
//
//        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
//
//        OAuthStateService stateService = new OAuthStateService(redisTemplate);
//
//        String state = stateService.createState("tenant-A", "user-123");
//
//        assertThat(state).isNotBlank();
//        assertThat(state).matches("[A-Za-z0-9_-]+")
//                .hasSizeGreaterThanOrEqualTo(20);
//
//        verify(hashOperations).put(
//                argThat(key -> key.startsWith("oauth:slack:state:")),
//                eq("tenantId"),
//                eq("tenant-A")
//        );
//        verify(hashOperations).put(
//                argThat(key -> key.startsWith("oauth:slack:state:")),
//                eq("userId"),
//                eq("user-123")
//        );
//        verify(redisTemplate).expire(
//                argThat(key -> key.startsWith("oauth:slack:state:")),
//                eq(Duration.ofMinutes(5))
//        );
//    }
//
//    @Test
//    void shouldBuildSlackAuthorizationUrlFromTenantContextAndGeneratedState() {
//        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
//        @SuppressWarnings("unchecked")
//        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
//
//        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
//
//        SlackOAuthProperties properties = new SlackOAuthProperties();
//        properties.setClientId("client-123");
//        properties.setScopes("chat:write,channels:read");
//        properties.setRedirectUri("https://example.com/slack/callback");
//
//        SlackOAuthClient slackOAuthClient = mock(SlackOAuthClient.class);
//        SlackOAuthService slackOAuthService = new SlackOAuthService(new OAuthStateService(redisTemplate), properties, slackOAuthClient, mock(SlackIntegrationService.class));
//
//        TenantContext.setTenantId("tenant-A");
//        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("user-123", null);
//
//        String authorizationUrl = slackOAuthService.createAuthorizationUrl(authentication);
//
//        assertThat(authorizationUrl)
//                .startsWith("https://slack.com/oauth/v2/authorize")
//                .contains("client_id=client-123")
//                .contains("scope=chat:write,channels:read")
//                .contains("redirect_uri=https://example.com/slack/callback")
//                .contains("state=");
//
//        verify(hashOperations).put(
//                argThat(key -> key.startsWith("oauth:slack:state:")),
//                eq("tenantId"),
//                eq("tenant-A")
//        );
//        verify(hashOperations).put(
//                argThat(key -> key.startsWith("oauth:slack:state:")),
//                eq("userId"),
//                eq("user-123")
//        );
//        verify(redisTemplate).expire(
//                argThat(key -> key.startsWith("oauth:slack:state:")),
//                eq(Duration.ofMinutes(5))
//        );
//    }
//
//    @Test
//    void shouldConsumeSlackOAuthState() {
//        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
//        @SuppressWarnings("unchecked")
//        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
//
//        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
//        when(hashOperations.entries(argThat(key -> key.startsWith("oauth:slack:state:"))))
//                .thenReturn(java.util.Map.of("tenantId", "tenant-001", "userId", "user-123"));
//
//        OAuthStateService stateService = new OAuthStateService(redisTemplate);
//
//        String state = stateService.createState("tenant-001", "user-123");
//        OAuthStateData data = stateService.consumeState(state);
//
//        assertEquals("tenant-001", data.tenantId());
//        assertEquals("user-123", data.userId());
//    }
//
//    @Test
//    void shouldRejectReusedSlackOAuthState() {
//        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
//        @SuppressWarnings("unchecked")
//        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
//
//        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
//
//        java.util.Map<String, Object>[] entriesByKey = new java.util.Map[] {
//                java.util.Map.of("tenantId", "tenant-001", "userId", "user-123"),
//                java.util.Collections.emptyMap()
//        };
//
//        doAnswer(invocation -> {
//            String key = invocation.getArgument(0);
//            return entriesByKey[0];
//        }).when(hashOperations).entries(anyString());
//
//        doAnswer(invocation -> {
//            entriesByKey[0] = java.util.Collections.emptyMap();
//            return null;
//        }).when(redisTemplate).delete(anyString());
//
//        OAuthStateService stateService = new OAuthStateService(redisTemplate);
//
//        String state = stateService.createState("tenant-001", "user-123");
//        stateService.consumeState(state);
//
//        assertThrows(
//                IllegalArgumentException.class,
//                () -> stateService.consumeState(state)
//        );
//    }
//
//    @Test
//    void shouldExchangeSlackOAuthCode() {
//        SlackOAuthClient slackOAuthClient = mock(SlackOAuthClient.class);
//        SlackOAuthService slackOAuthService = new SlackOAuthService(
//                mock(OAuthStateService.class),
//                new SlackOAuthProperties(),
//                slackOAuthClient,
//                mock(SlackIntegrationService.class)
//        );
//
//        SlackOAuthAccessResponse response = new SlackOAuthAccessResponse(
//                true,
//                "xoxb-test-token",
//                "bot",
//                "channels:read",
//                "U123",
//                "A123",
//                new SlackOAuthAccessResponse.Team("T123", "Test Workspace"),
//                null
//        );
//
//        when(slackOAuthClient.exchangeCode("test-code")).thenReturn(response);
//
//        SlackOAuthAccessResponse result = slackOAuthService.exchangeCode("test-code");
//
//        assertThat(result.ok()).isTrue();
//        assertThat(result.accessToken()).isEqualTo("xoxb-test-token");
//        assertThat(result.team().id()).isEqualTo("T123");
//        assertThat(result.team().name()).isEqualTo("Test Workspace");
//
//        verify(slackOAuthClient).exchangeCode("test-code");
//    }
//
//    @Test
//    void shouldRejectFailedSlackOAuthExchange() {
//        SlackOAuthClient slackOAuthClient = mock(SlackOAuthClient.class);
//        SlackOAuthService slackOAuthService = new SlackOAuthService(
//                mock(OAuthStateService.class),
//                new SlackOAuthProperties(),
//                slackOAuthClient,
//                mock(SlackIntegrationService.class)
//        );
//
//        SlackOAuthAccessResponse response = new SlackOAuthAccessResponse(
//                false,
//                null,
//                null,
//                null,
//                null,
//                null,
//                null,
//                "invalid_code"
//        );
//
//        when(slackOAuthClient.exchangeCode("bad-code")).thenReturn(response);
//
//        assertThrows(
//                IllegalStateException.class,
//                () -> slackOAuthService.exchangeCode("bad-code")
//        );
//    }
//
//    @Test
//    void shouldHandleSlackOAuthCallback() {
//        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
//        @SuppressWarnings("unchecked")
//        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
//
//        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
//        when(hashOperations.entries(argThat(key -> key.startsWith("oauth:slack:state:"))))
//                .thenReturn(java.util.Map.of("tenantId", "tenant-001", "userId", "user-123"));
//
//        OAuthStateService stateService = new OAuthStateService(redisTemplate);
//        SlackOAuthClient slackOAuthClient = mock(SlackOAuthClient.class);
//        SlackIntegrationService slackIntegrationService = mock(SlackIntegrationService.class);
//        SlackOAuthService slackOAuthService = new SlackOAuthService(stateService, new SlackOAuthProperties(), slackOAuthClient, slackIntegrationService);
//
//        String state = stateService.createState("tenant-001", "user-123");
//
//        SlackOAuthAccessResponse response = new SlackOAuthAccessResponse(
//                true,
//                "xoxb-test-token",
//                "bot",
//                "channels:read",
//                "U123",
//                "A123",
//                new SlackOAuthAccessResponse.Team("T123", "Test Workspace"),
//                null
//        );
//
//        when(slackOAuthClient.exchangeCode("test-code")).thenReturn(response);
//
//        var result = slackOAuthService.handleCallback("test-code", state);
//
//        assertThat(result.tenantId()).isEqualTo("tenant-001");
//        assertThat(result.userId()).isEqualTo("user-123");
//        assertThat(result.slackResponse().team().id()).isEqualTo("T123");
//
//        verify(slackOAuthClient).exchangeCode("test-code");
//        verify(slackIntegrationService).createFromOAuth("tenant-001", response);
//    }
//
//    @Test
//    void shouldRejectInvalidSlackOAuthState() {
//        SlackOAuthClient slackOAuthClient = mock(SlackOAuthClient.class);
//        OAuthStateService stateService = mock(OAuthStateService.class);
//        SlackIntegrationService slackIntegrationService = mock(SlackIntegrationService.class);
//        SlackOAuthService slackOAuthService = new SlackOAuthService(stateService, new SlackOAuthProperties(), slackOAuthClient, slackIntegrationService);
//
//        when(stateService.consumeState("invalid-state"))
//                .thenThrow(new IllegalArgumentException("invalid or expired OAuth state"));
//
//        assertThrows(
//                IllegalArgumentException.class,
//                () -> slackOAuthService.handleCallback("test-code", "invalid-state")
//        );
//
//        verifyNoInteractions(slackOAuthClient);
//    }
//
//    @Test
//    void shouldCreateSlackIntegrationFromOAuth() {
//        SlackIntegrationRepository repository = mock(SlackIntegrationRepository.class);
//        CredentialEncryptionService encryptionService = mock(CredentialEncryptionService.class);
//        SlackOAuthProperties oauthProperties = new SlackOAuthProperties();
//        oauthProperties.setSigningSecret("your-slack-signing-secret");
//
//        when(repository.findByTeamId("T123")).thenReturn(Optional.empty());
//        when(encryptionService.encrypt("xoxb-test-token")).thenReturn("encrypted-token");
//        when(encryptionService.encrypt("your-slack-signing-secret")).thenReturn("encrypted-signing-secret");
//        when(repository.save(any(SlackIntegration.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        SlackIntegrationService integrationService = new SlackIntegrationService(repository, encryptionService, oauthProperties);
//
//        SlackOAuthAccessResponse response = new SlackOAuthAccessResponse(
//                true,
//                "xoxb-test-token",
//                "bot",
//                "channels:read",
//                "U123",
//                "A123",
//                new SlackOAuthAccessResponse.Team("T123", "Test Workspace"),
//                null
//        );
//
//        SlackIntegration result = integrationService.createFromOAuth("tenant-001", response);
//
//        assertThat(result.getTenantId()).isEqualTo("tenant-001");
//        assertThat(result.getTeamId()).isEqualTo("T123");
//        assertThat(result.getTeamName()).isEqualTo("Test Workspace");
//        assertThat(result.getBotUserId()).isEqualTo("U123");
//        assertThat(result.getSigningSecret()).isNotNull();
//        assertThat(result.getAccessTokenEncrypted()).isNotNull();
//        assertThat(result.getAccessTokenEncrypted()).isNotEqualTo("xoxb-test-token");
//        assertThat(result.getSigningSecret()).isNotEqualTo("your-slack-signing-secret");
//
//        verify(repository).save(any(SlackIntegration.class));
//    }
//}
