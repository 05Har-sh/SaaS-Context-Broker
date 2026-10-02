package com.harsh.context_broker.contextBroker.controller.oauth;

import com.harsh.context_broker.contextBroker.frontend.AppProperties;
import com.harsh.context_broker.contextBroker.oauth.slackstate.OAuthStateService;
import com.harsh.context_broker.contextBroker.service.oauth.SlackOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/integrations/slack/oauth")
public class SlackOAuthController {
    private final SlackOAuthService slackOAuthService;
    private final AppProperties appProperties;
    private final OAuthStateService stateService;

    public SlackOAuthController(SlackOAuthService slackOAuthService, AppProperties appProperties, OAuthStateService stateService) {
        this.slackOAuthService = slackOAuthService;
        this.appProperties = appProperties;
        this.stateService = stateService;
    }

    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorize(Authentication authentication) {
        String authorizationUrl = slackOAuthService.createAuthorizationUrl(authentication);
        return  ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
    }


    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {

        if (error != null) {
            if (state == null || state.isBlank()) {
                return redirectToFrontend("failed");
            }
            try {
                stateService.consumeState(state);
            } catch (IllegalArgumentException ex) {
                return redirectToFrontend("failed");
            }
            return redirectToFrontend(
                    "access_denied".equals(error) ? "cancelled" : "failed"
            );
        }
        if (code == null || code.isBlank()
                || state == null || state.isBlank()) {
            return redirectToFrontend("failed");
        }
        try {
            slackOAuthService.handleCallback(code, state);
            return redirectToFrontend("connected");

        } catch (IllegalArgumentException | IllegalStateException ex) {
            return redirectToFrontend("failed");
        }
    }
    private ResponseEntity<Void> redirectToFrontend(String status) {

        URI redirectUri = UriComponentsBuilder
                .fromUriString(appProperties.getFrontendUrl())
                .path("/integrations")
                .queryParam("slack", status)
                .build()
                .toUri();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }
}
