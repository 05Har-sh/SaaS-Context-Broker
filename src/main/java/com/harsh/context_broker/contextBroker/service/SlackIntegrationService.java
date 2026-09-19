package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
import org.springframework.stereotype.Service;

@Service
public class SlackIntegrationService {
    private final SlackIntegrationRepository repository;

    public SlackIntegrationService(SlackIntegrationRepository repository) {
        this.repository = repository;
    }
    public SlackIntegration getByTeamId(String teamId) {
        return repository.findByTeamId(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown Slack team"));
    }
}
