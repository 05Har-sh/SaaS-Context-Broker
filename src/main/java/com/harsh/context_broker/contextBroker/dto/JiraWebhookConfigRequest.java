package com.harsh.context_broker.contextBroker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class JiraWebhookConfigRequest {
    @NotBlank(message = "Webhook url must not be blank")
    @Pattern(regexp = "https?://.+", message = "Invalid Jira webhookUrl")
    private String webhookUrl;

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
