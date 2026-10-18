package com.harsh.context_broker.contextBroker.repository;


import com.harsh.context_broker.contextBroker.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, String> {
}
