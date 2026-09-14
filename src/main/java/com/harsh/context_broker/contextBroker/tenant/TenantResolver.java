package com.harsh.context_broker.contextBroker.tenant;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * 1. Get Authentication from SecurityContextHolder
 * 2. Check that it is a JWT authentication
 * 3. Get the Jwt
 * 4. Ask TenantResolver for the tenant ID
 * 5. Put tenant ID into TenantContext
 * 6. Continue the request
 * 7. Clear TenantContext when the request finishes
 */

@Component
public class TenantResolver {
    public static final String TENANT_ID_CLAIM = "tenant_id";

    public String resolveTenant(Jwt jwt) {
        if(jwt == null) {
            throw new TenantException("Authenticated JWT is missing");
        }
        String tenantId = jwt.getClaimAsString(TENANT_ID_CLAIM);

        if(tenantId == null || tenantId.isBlank()) {
            throw new TenantException("Tenant ID is missing from the authenticated JWT");
        }
        return tenantId;
    }
}
