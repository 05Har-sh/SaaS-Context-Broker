package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.WebhookConfig;
import com.harsh.context_broker.contextBroker.repository.WebhookConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class WebhookConfigService {

    private final WebhookConfigRepository repository;


    public WebhookConfigService(WebhookConfigRepository repository) {
        this.repository = repository;
    }

    public void saveWebhook(String url){
        WebhookConfig config = repository.findAll()
                .stream()
                .findFirst()
                .orElse(new WebhookConfig());

        config.setWebhookUrl(url);
        repository.save(config);
    }
    public String getWebhook(){
        return repository.findAll()
                .stream()
                .findFirst()
                .map(WebhookConfig::getWebhookUrl)
                .orElse(null);
    }
}
