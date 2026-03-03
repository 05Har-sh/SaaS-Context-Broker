package com.harsh.context_broker.contextBroker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class WebhookConfigRequest {

    @NotBlank(message = "Webhook url must not be blank")
    @Pattern(
            regexp = "https://hooks.slack.com/services/.*",
            message = "Invalid Slack webhook URL"
    )
    private String webhookUrl;

    public @NotBlank(message = "Webhook url must not be blank") @Pattern(
            regexp = "https://hooks.slack.com/services/.*",
            message = "Invalid Slack webhook URL"
    ) String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(@NotBlank(message = "Webhook url must not be blank") @Pattern(
            regexp = "https://hooks.slack.com/services/.*",
            message = "Invalid Slack webhook URL"
    ) String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
