package com.harsh.context_broker.contextBroker.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 3, max = 100)
        String username,

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotBlank
        @Size(min = 12, max = 128)
        String password,

        //name of the new customer's workspace/tenant.
        @NotBlank
        @Size(min = 2, max = 255)
        String tenantName
) {}