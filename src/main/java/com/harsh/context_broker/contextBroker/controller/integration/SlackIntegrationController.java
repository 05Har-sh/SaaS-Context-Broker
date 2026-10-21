package com.harsh.context_broker.contextBroker.controller.integration;

import com.harsh.context_broker.contextBroker.dto.integration.SlackIntegrationRequest;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.service.SlackIntegrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        SlackIntegration integration = integrationService.getCurrentTenantIntegration();
        if (integration == null || !integration.isActive()) {
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "workspaceName", null,
                    "teamId", null
            ));
        }

        return ResponseEntity.ok(Map.of(
                "connected", integration.getAccessTokenEncrypted() != null && !integration.getAccessTokenEncrypted().isBlank(),
                "workspaceName", integration.getTeamName() != null ? integration.getTeamName() : integration.getTeamId(),
                "teamId", integration.getTeamId()
        ));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> disconnect() {
        SlackIntegration integration = integrationService.getCurrentTenantIntegration();
        if (integration == null) {
            return ResponseEntity.ok(Map.of(
                    "connected", false,
                    "status", "not_connected"
            ));
        }

        integrationService.disconnect(integration.getId());
        return ResponseEntity.ok(Map.of(
                "connected", false,
                "status", "disconnected",
                "workspaceName", integration.getTeamName() != null ? integration.getTeamName() : integration.getTeamId()
        ));
    }
}
