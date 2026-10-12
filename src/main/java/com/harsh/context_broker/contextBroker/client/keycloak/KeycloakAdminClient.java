package com.harsh.context_broker.contextBroker.client.keycloak;


import com.harsh.context_broker.contextBroker.config.keycloak.KeycloakAdminProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class KeycloakAdminClient {

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getServerUrl())
                .build();
    }

    public String getAdminAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        Map<?, ?> response = restClient.post()
                .uri("/realms/context-broker-dev/protocol/openid-connect/token",
                        properties.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException(
                    "Keycloak did not return an admin access token"
            );
        }

        return response.get("access_token").toString();
    }

    public String createUser(
            String email,
            String username,
            String firstName,
            String lastName,
            String password,
            String tenantId
    ) {
        String accessToken = getAdminAccessToken();

        Map<String, Object> user = Map.of(
                "username", username,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", false,
                "attributes", Map.of(
                        "tenant_id", List.of(tenantId)
                ),
                "credentials", List.of(
                        Map.of(
                                "type", "password",
                                "value", password,
                                "temporary", false
                        )
                )
        );

        var response = restClient.post()
                .uri("/admin/realms/context-broker-dev/users", properties.getRealm())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(user)
                .exchange((request, result) -> {
                    if (result.getStatusCode().is2xxSuccessful()) {
                        return result.getHeaders().getLocation();
                    }

                    throw new IllegalStateException(
                            "Keycloak user creation failed with HTTP "
                                    + result.getStatusCode().value()
                    );
                });

        if (response == null || response.getPath() == null) {
            throw new IllegalStateException(
                    "Keycloak created no user location in its response"
            );
        }

        String path = response.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }


    public void assignClientRole(
            String userId,
            String roleName
    ) {
        String accessToken = getAdminAccessToken();

        // Find the context-broker client UUID.
//        List<Map<String, Object>> clients = restClient.get()
//                .uri("/admin/realms/context-broker-dev/clients?clientId={clientId}",
//                        properties.getRealm(), "context-broker")
//                .header("Authorization", "Bearer " + accessToken)
//                .retrieve()
//                .body(new org.springframework.core.ParameterizedTypeReference<>() {});
//
//        if (clients == null || clients.isEmpty()) {
//            throw new IllegalStateException(
//                    "Keycloak client 'context-broker' was not found"
//            );
//        }


        String realm = properties.getRealm();

        System.out.println("Keycloak realm: " + realm);
        System.out.println("Looking up application client: context-broker");

        List<Map<String, Object>> clients = restClient.get()
                .uri("/admin/realms/{realm}/clients?clientId={clientId}",
                        realm, "context-broker")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException(
                    "Client lookup returned no results. Realm queried: " + realm
            );
        }

        System.out.println("Matching clients found: " + clients.size());
        System.out.println("Matched client UUID: " + clients.get(0).get("id"));

        String clientUuid = clients.get(0).get("id").toString();


        //Object clientUuid = clients.get(0).get("id");

        // Find the role by its name.
        System.out.println("STEP A: Fetching client role: " + roleName);
        Map<String, Object> role = restClient.get()
                .uri("/admin/realms/{realm}/clients/{clientUuid}/roles/{roleName}",
                        properties.getRealm(), clientUuid, roleName)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});


        if (role == null || role.get("id") == null) {
            throw new IllegalStateException(
                    "Keycloak client role not found: " + roleName
            );
        }
        System.out.println("STEP B: Role found: " + role.get("name"));

        // Assign the role to the user.
        System.out.println("STEP C: Assigning role to user: " + userId);
        restClient.post()
                .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUuid}",
                        properties.getRealm(), userId, clientUuid)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
        System.out.println("STEP D: Role assignment succeeded");
    }


}

