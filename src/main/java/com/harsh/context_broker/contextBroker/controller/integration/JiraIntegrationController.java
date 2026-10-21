package com.harsh.context_broker.contextBroker.controller.integration;

import com.harsh.context_broker.contextBroker.dto.integration.JiraIntegrationRequest;
import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.service.JiraIntegrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        List<JiraIntegration> integrations = integrationService.getCurrentTenantIntegrations();
        List<Map<String, Object>> payload = integrations.stream()
                .map(integration -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("id", integration.getId());
                    row.put("siteName", integration.getSiteName() != null ? integration.getSiteName() : integration.getCloudId());
                    row.put("siteUrl", integration.getSiteUrl());
                    row.put("status", integration.getConnectionStatus() != null ? integration.getConnectionStatus().name() : "DISCONNECTED");
                    row.put("webhookHealth", integration.getWebhookHealth() != null ? integration.getWebhookHealth().name() : "HEALTHY");
                    row.put("cloudId", integration.getCloudId());
                    row.put("createdAt", integration.getCreatedAt());
                    row.put("updatedAt", integration.getUpdatedAt());
                    return row;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "connected", !payload.isEmpty(),
                "count", payload.size(),
                "integrations", payload
        ));
    }

    @DeleteMapping("/{integrationId}")
    public ResponseEntity<Map<String, Object>> disconnect(@PathVariable Long integrationId) {
        JiraIntegration integration = integrationService.disconnect(integrationId);
        return ResponseEntity.ok(Map.of(
                "disconnected", true,
                "integrationId", integration.getId(),
                "status", integration.getConnectionStatus().name()
        ));
    }
}
