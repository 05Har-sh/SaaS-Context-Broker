package com.harsh.context_broker.contextBroker.controller.integration;

import com.harsh.context_broker.contextBroker.dto.integration.IntegrationSummary;
import com.harsh.context_broker.contextBroker.service.IntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/integrations")
public class IntegrationController {
    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationSummary>> list() {
        List<IntegrationSummary> summaries = integrationService.getAllIntegrations();
        return ResponseEntity.ok(summaries);
    }
}