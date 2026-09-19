package com.harsh.context_broker.contextBroker.repository;

import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlackIntegrationRepository extends JpaRepository<SlackIntegration, Long> {
    Optional<SlackIntegration> findByTeamId(String teamId);
}
