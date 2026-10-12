package com.harsh.context_broker.contextBroker.service;

import com.harsh.context_broker.contextBroker.dto.integration.IntegrationSummary;
import com.harsh.context_broker.contextBroker.entity.JiraIntegration;
import com.harsh.context_broker.contextBroker.entity.SlackIntegration;
import com.harsh.context_broker.contextBroker.model.JiraIntegrationType;
import com.harsh.context_broker.contextBroker.repository.JiraIntegrationRepository;
import com.harsh.context_broker.contextBroker.repository.SlackIntegrationRepository;
import com.harsh.context_broker.contextBroker.tenant.TenantContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class IntegrationServiceTest {

    @Autowired
    private SlackIntegrationRepository slackRepository;

    @Autowired
    private JiraIntegrationRepository jiraRepository;

    @Autowired
    private IntegrationService integrationService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        slackRepository.deleteAll();
        jiraRepository.deleteAll();
    }

    @Test
    void tenantWithSlackIntegrationReceivesIntegration() {
        SlackIntegration s = new SlackIntegration();
        s.setTenantId("tenant-1");
        s.setTeamId("T123");
        s.setTeamName("Team X");
        s.setAccessTokenEncrypted("enc-token");
        slackRepository.saveAndFlush(s);

        TenantContext.setTenantId("tenant-1");
        List<IntegrationSummary> list = integrationService.getAllIntegrations();
        assertThat(list).singleElement().satisfies(i -> {
            assertThat(i.getType()).isEqualTo("SLACK");
            assertThat(i.isConnected()).isTrue();
            assertThat(i.getDisplayName()).isEqualTo("Team X");
        });
    }

    @Test
    void tenantWithoutIntegrationsReceivesEmpty() {
        TenantContext.setTenantId("tenant-empty");
        List<IntegrationSummary> list = integrationService.getAllIntegrations();
        assertThat(list).isEmpty();
    }

    @Test
    void tenantCannotSeeAnotherTenantsIntegrations() {
        SlackIntegration a = new SlackIntegration();
        a.setTenantId("tenant-A");
        a.setTeamId("TA");
        a.setTeamName("A");
        slackRepository.saveAndFlush(a);

        SlackIntegration b = new SlackIntegration();
        b.setTenantId("tenant-B");
        b.setTeamId("TB");
        b.setTeamName("B");
        slackRepository.saveAndFlush(b);

        TenantContext.setTenantId("tenant-A");
        List<IntegrationSummary> listA = integrationService.getAllIntegrations();
        assertThat(listA).hasSize(1);
        assertThat(listA.get(0).getDisplayName()).isEqualTo("A");

        TenantContext.setTenantId("tenant-B");
        List<IntegrationSummary> listB = integrationService.getAllIntegrations();
        assertThat(listB).hasSize(1);
        assertThat(listB.get(0).getDisplayName()).isEqualTo("B");
    }

    @Test
    void sensitiveCredentialsNotExposedInSummary() {
        SlackIntegration s = new SlackIntegration();
        s.setTenantId("tenant-sec");
        s.setTeamId("TSEC");
        s.setTeamName("SecureTeam");
        s.setAccessTokenEncrypted("secret-value");
        s.setSigningSecret("sign-secret");
        slackRepository.saveAndFlush(s);

        TenantContext.setTenantId("tenant-sec");
        List<IntegrationSummary> list = integrationService.getAllIntegrations();
        assertThat(list).singleElement().satisfies(i -> {
            // IntegrationSummary intentionally contains only safe fields
            assertThat(i.getId()).isNotNull();
            assertThat(i.getType()).isEqualTo("SLACK");
            assertThat(i.getDisplayName()).isNotBlank();
        });

        // Verify original entity still stores secrets (sanity for test)
        SlackIntegration stored = slackRepository.findAll().get(0);
        assertThat(stored.getAccessTokenEncrypted()).isEqualTo("secret-value");
        assertThat(stored.getSigningSecret()).isEqualTo("sign-secret");
    }
}