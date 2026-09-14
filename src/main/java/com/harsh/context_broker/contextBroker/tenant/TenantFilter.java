package com.harsh.context_broker.contextBroker.tenant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class TenantFilter extends OncePerRequestFilter {
    private final TenantResolver tenantResolver;

    public TenantFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                String tenantId = tenantResolver.resolveTenant(jwtAuthenticationToken.getToken());
                TenantContext.setTenantId(tenantId);

            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
