package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.dto.ApiSuccessResponse;
import com.harsh.context_broker.contextBroker.dto.WebhookConfigRequest;
import com.harsh.context_broker.contextBroker.service.WebhookConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class WebhookConfigController {
    private final WebhookConfigService service;

    public WebhookConfigController(WebhookConfigService service) {
        this.service = service;
    }
    @PostMapping("/slack")
    public ApiSuccessResponse saveWebhook(@Valid @RequestBody WebhookConfigRequest request){

        service.saveWebhook(request.getWebhookUrl());
        return new ApiSuccessResponse("Slack Webhook saved succesfully");
    }
}
