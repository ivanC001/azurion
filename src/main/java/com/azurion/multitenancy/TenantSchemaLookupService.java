package com.azurion.multitenancy;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import com.azurion.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantSchemaLookupService {

    private static final long TTL_SECONDS = 300;

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, CacheItem> cache = new ConcurrentHashMap<>();

    public String resolveSchema(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenantId)) {
            return TenantContext.DEFAULT_TENANT;
        }

        CacheItem cached = cache.get(tenantId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.schemaName();
        }

        String schema = jdbcTemplate.query(
                "SELECT schema_name FROM public.schemas_empresas WHERE tenant_id = ? AND active = TRUE",
                rs -> rs.next() ? rs.getString(1) : null,
                tenantId
        );

        if (schema == null || schema.isBlank()) {
            throw new BusinessException("TENANT_NO_ENCONTRADO", "El tenant solicitado no existe o esta inactivo");
        }

        cache.put(tenantId, new CacheItem(schema, Instant.now().plusSeconds(TTL_SECONDS)));
        return schema;
    }

    private record CacheItem(String schemaName, Instant expiresAt) {
    }
}
