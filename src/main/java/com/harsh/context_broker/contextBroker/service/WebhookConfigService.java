package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.WebhookConfig;
import com.harsh.context_broker.contextBroker.repository.WebhookConfigRepository;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.springframework.stereotype.Service;

/**
 * Tenant A request
 *       ↓
 * TenantContext.requireTenantId()
 *       ↓
 * tenant-A
 *       ↓
 * findByTenantId("tenant-A")
 *       ↓
 * Tenant A's config
 */

@Service
public class WebhookConfigService {

    private final WebhookConfigRepository repository;


    public WebhookConfigService(WebhookConfigRepository repository) {
        this.repository = repository;
    }

    public void saveWebhook(String url){  // save Slack webhookUrl

        String tenantId = TenantContext.requireTenantId();

        WebhookConfig config = repository.findByTenantId(tenantId)
                        .orElseGet(() -> {
                            WebhookConfig newConfig = new WebhookConfig();
                            newConfig.setTenantId(tenantId);
                            return newConfig;
                        });

        config.setWebhookUrl(url);
        repository.save(config);
    }

    public void saveJiraWebhookUrl(String url){
        String tenantId = TenantContext.requireTenantId();

        WebhookConfig config = repository.findByTenantId(tenantId)
                        .orElseGet(() -> {
                            WebhookConfig newConfig = new WebhookConfig();
                            newConfig.setTenantId(tenantId);
                            return newConfig;
                        });

        config.setJiraWebhookUrl(url);
        repository.save(config);
    }

    public String getWebhook(){
        String tenantId = TenantContext.requireTenantId();
        return repository.findByTenantId(tenantId)
                .map(WebhookConfig::getWebhookUrl)
                .orElse(null);
    }

    public String getJiraWebhook(){
        String tenantId = TenantContext.requireTenantId();
        return repository.findByTenantId(tenantId)
                .map(WebhookConfig::getJiraWebhookUrl)
                .orElse(null);
    }
}
