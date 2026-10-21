package com.harsh.context_broker.contextBroker.controller.oauth;

import com.harsh.context_broker.contextBroker.frontend.AppProperties;
import com.harsh.context_broker.contextBroker.service.oauth.JiraOAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JiraOAuthControllerTest {

    @Test
    void shouldRedirectToConfiguredFrontendOnSuccess() {
        JiraOAuthService service = mock(JiraOAuthService.class);
        AppProperties properties = new AppProperties();
        properties.setFrontendUrl("https://app.example.com");
        JiraOAuthController controller = new JiraOAuthController(service, properties);

        ResponseEntity<Void> response = controller.callback("code-123", "state-456", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .isEqualTo("https://app.example.com/integrations?jira=connected");
        verify(service).handleCallback("code-123", "state-456");
    }

    @Test
    void shouldRedirectToConfiguredFrontendOnFailure() {
        JiraOAuthService service = mock(JiraOAuthService.class);
        AppProperties properties = new AppProperties();
        properties.setFrontendUrl("https://app.example.com");
        JiraOAuthController controller = new JiraOAuthController(service, properties);
        doThrow(new IllegalStateException("boom")).when(service).handleCallback("bad-code", "bad-state");

        ResponseEntity<Void> response = controller.callback("bad-code", "bad-state", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().toString())
                .isEqualTo("https://app.example.com/integrations?jira=failed");
    }
}
