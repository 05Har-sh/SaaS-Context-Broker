package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.entity.JiraWebhookEvent;
import com.harsh.context_broker.contextBroker.repository.JiraWebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class JiraWebhookEventServiceTest {
    @Autowired
    private JiraWebhookRepository repository;

    @Autowired
    private JiraWebhookEventService service;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldClaimNewWebhookOnlyOncePerTenantAndIdentifier() {
        assertThat(service.claimWebhook("tenant-a", "hook-123")).isTrue();
        assertThat(service.claimWebhook("tenant-a", "hook-123")).isFalse();
        assertThat(repository.count()).isEqualTo(1);

        JiraWebhookEvent event = repository.findAll().get(0);
        assertThat(event.getTenantId()).isEqualTo("tenant-a");
        assertThat(event.getWebhookIdentifier()).isEqualTo("hook-123");
        assertThat(event.getReceivedAt()).isNotNull();
    }

    @Test
    void shouldAllowTheSameIdentifierAcrossDifferentTenants() {
        assertThat(service.claimWebhook("tenant-a", "hook-123")).isTrue();
        assertThat(service.claimWebhook("tenant-b", "hook-123")).isTrue();

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicateRecordForSameTenantAndIdentifier() {
        service.record("tenant-a", "hook-123");

        assertThatThrownBy(() -> service.record("tenant-a", "hook-123"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTenantId()).isEqualTo("tenant-a");
                    assertThat(event.getWebhookIdentifier()).isEqualTo("hook-123");
                });
    }
}
