package com.harsh.context_broker.contextBroker.controller.oauth;

import com.harsh.context_broker.contextBroker.service.oauth.JiraOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/integrations/jira/oauth")
public class JiraOAuthController {
    private static final Logger log = LoggerFactory.getLogger(JiraOAuthController.class);
    private final JiraOAuthService jiraOAuthService;

    public JiraOAuthController(JiraOAuthService jiraOAuthService) {
        this.jiraOAuthService = jiraOAuthService;
    }
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorize(Authentication authentication) {
        log.info("Received Jira OAuth authorize request for user={}", authentication != null ? authentication.getName() : "anonymous");
        String authorizationUrl = jiraOAuthService.createAuthorizationUrl(authentication);

        log.info("Generated Jira OAuth authorization URL for user={}: {}", authentication != null ? authentication.getName() : "anonymous", authorizationUrl);
        return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
    }
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        log.info("Jira OAuth callback received: codePresent={}, statePresent={}, error={}",
                code != null && !code.isBlank(),
                state != null && !state.isBlank(),
                error);

        if (error != null) {
            log.warn("Jira OAuth callback failed because Atlassian returned an error: {}", error);
            return redirectToFrontend("cancelled");
        }

        try {
            jiraOAuthService.handleCallback(code, state);
            log.info("Jira OAuth callback completed successfully. Redirecting to frontend as connected.");
            return redirectToFrontend("connected");

        } catch (Exception e) {
            log.error("Jira OAuth callback failed during Jira connect flow: codePresent={}, statePresent={}, error={}",
                    code != null && !code.isBlank(),
                    state != null && !state.isBlank(),
                    e.getMessage(),
                    e);
            return redirectToFrontend("failed");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String status) {

        URI location = URI.create(
                "http://localhost:3000/integrations?jira=" + status
        );

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location)
                .build();
    }
}

