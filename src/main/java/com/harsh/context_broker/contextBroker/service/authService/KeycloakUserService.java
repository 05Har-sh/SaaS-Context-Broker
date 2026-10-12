package com.harsh.context_broker.contextBroker.service.authService;


import com.harsh.context_broker.contextBroker.client.keycloak.KeycloakAdminClient;
import org.springframework.stereotype.Service;

@Service
public class KeycloakUserService {

    private final KeycloakAdminClient keycloakAdminClient;

    public KeycloakUserService(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    //makes the first user an ADMIN by backend decision, rather than trusting a role submitted by the client.
    public String registerUser(
            String email,
            String username,
            String firstName,
            String lastName,
            String password,
            String tenantId
    ) {
        String userId = keycloakAdminClient.createUser(
                email,
                username,
                firstName,
                lastName,
                password,
                tenantId
        );

        keycloakAdminClient.assignClientRole(userId, "ADMIN");

        return userId;
    }
}
