package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.AlertResponse;
import com.harsh.context_broker.contextBroker.entity.IncidentEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SlackNotifier {

    private final RestTemplate restTemplate = new RestTemplate();
    private final WebhookConfigService configService;

    public SlackNotifier(WebhookConfigService configService) {
        this.configService = configService;
    }

    public void sendAlert(String message) {

        String webhookUrl = configService.getWebhook();

        if (webhookUrl == null) {
            System.out.println("⚠ No Slack webhook configured");
            return;
        }

        Map<String, String> payload = Map.of("text", message);

        restTemplate.postForObject(webhookUrl, payload, String.class);
    }

    public void sendAlert(IncidentEntity incident, AlertResponse alert) {

        String webhookUrl = configService.getWebhook();

        if (webhookUrl == null) {
            System.out.println("⚠ No Slack webhook configured");
            return;
        }
        String message = buildSlackMessage(incident, alert);

        Map<String, String> payload = Map.of("text", message);
        restTemplate.postForObject(webhookUrl, payload, String.class);
    }

    private String buildSlackMessage(IncidentEntity incident, AlertResponse alert) {
        return """
                🚨 *Incident Alert*
                
                *Key:* %s
                *Severity:* %s
                *Score:* %d
                
                *Reason:* %s
                """.formatted(
                incident.getIncidentKey(),
                alert.getSeverity(),
                alert.getScore(),
                alert.getReason()
        );
    }

}