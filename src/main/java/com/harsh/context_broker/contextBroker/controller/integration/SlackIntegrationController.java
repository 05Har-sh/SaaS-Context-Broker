package com.harsh.context_broker.contextBroker.controller.integration;

import com.harsh.context_broker.contextBroker.dto.integration.SlackIntegrationRequest;
import com.harsh.context_broker.contextBroker.service.SlackIntegrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integrations/slack")
public class SlackIntegrationController {
    private final SlackIntegrationService integrationService;

    public SlackIntegrationController(SlackIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> create(@RequestBody SlackIntegrationRequest request) {
        integrationService.create(request.getTeamId(), request.getSigningSecret());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
