package com.harsh.context_broker.contextBroker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Map;

@SpringBootTest
class SaaSContextBrokerApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class SecurityTestConfig {
		@Bean
		JwtDecoder jwtDecoder() {
			return token -> Jwt.withTokenValue(token)
					.header("alg", "none")
					.claim("sub", "test-user")
					.claim("scope", "test")
					.claim("resource_access", Map.of("context-broker", Map.of("roles", java.util.List.of("USER"))))
					.build();
		}
	}
}
