package com.harsh.context_broker.contextBroker.service.authService;

import com.harsh.context_broker.contextBroker.dto.auth.RegisterRequest;
import com.harsh.context_broker.contextBroker.entity.Tenant;
import com.harsh.context_broker.contextBroker.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
/*
Generates a tenant ID on the backend.
Saves the tenant in PostgreSQL.
Creates the first user in Keycloak with the tenant_id attribute.
Assigns that user the ADMIN client role.
Returns the generated tenant ID.
 */
@Service
public class RegistrationService {
    private final TenantRepository tenantRepository;
    private final KeycloakUserService keycloakUserService;

    public RegistrationService(TenantRepository tenantRepository, KeycloakUserService keycloakUserService) {
        this.tenantRepository = tenantRepository;
        this.keycloakUserService = keycloakUserService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String tenantId = UUID.randomUUID().toString();
        Tenant tenant = new Tenant(tenantId, request.tenantName());

        tenantRepository.save(tenant);

        keycloakUserService.registerUser(request.email(),  request.username(), request.firstName(), request.lastName(), request.password(), tenantId);
        return tenantId;
    }
}
