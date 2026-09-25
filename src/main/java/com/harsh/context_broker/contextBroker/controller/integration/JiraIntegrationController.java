package com.harsh.context_broker.contextBroker.controller.integration;

import com.harsh.context_broker.contextBroker.dto.integration.JiraIntegrationRequest;
import com.harsh.context_broker.contextBroker.service.JiraIntegrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integrations/jira")
public class JiraIntegrationController {
    private final JiraIntegrationService integrationService;

    public JiraIntegrationController(JiraIntegrationService integrationService) {
        this.integrationService = integrationService;
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> create(@RequestBody JiraIntegrationRequest request) {
        integrationService.create(request.getJiraWebhookId(), request.getWebhookSecret());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
