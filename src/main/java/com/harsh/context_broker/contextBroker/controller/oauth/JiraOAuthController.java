package com.harsh.context_broker.contextBroker.controller.oauth;

import com.harsh.context_broker.contextBroker.service.oauth.JiraOAuthService;
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
    private final JiraOAuthService jiraOAuthService;

    public JiraOAuthController(JiraOAuthService jiraOAuthService) {
        this.jiraOAuthService = jiraOAuthService;
    }
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorize(Authentication authentication) {
        String authorizationUrl = jiraOAuthService.createAuthorizationUrl(authentication);

        return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
    }
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {

        if (error != null) {
            return redirectToFrontend("cancelled");
        }

        try {
            jiraOAuthService.handleCallback(code, state);
            return redirectToFrontend("connected");

        } catch (Exception e) {
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

