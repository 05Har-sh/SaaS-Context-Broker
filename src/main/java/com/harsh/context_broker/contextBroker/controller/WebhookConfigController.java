package com.harsh.context_broker.contextBroker.controller;

import com.harsh.context_broker.contextBroker.service.WebhookConfigService;
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
    public String saveWebhook(@RequestBody Map<String, String> body){
        String url = body.get("webhookUrl");
        service.saveWebhook(url);
        return "✅ Slack webhook saved";
    }
}
