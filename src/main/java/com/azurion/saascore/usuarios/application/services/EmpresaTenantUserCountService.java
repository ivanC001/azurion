package com.azurion.saascore.usuarios.application.services;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.shared.exception.BusinessException;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmpresaTenantUserCountService {

    private static final Pattern SAFE_SCHEMA = Pattern.compile("[a-z][a-z0-9_]{2,62}");

    private final JdbcTemplate jdbcTemplate;

    public long countActiveUsers(Empresa empresa) {
        return countUsers(empresa).active();
    }

    public TenantUserCounts countUsers(Empresa empresa) {
        String schema = validatedSchema(empresa);
        return jdbcTemplate.queryForObject(
                """
                select count(*) as total,
                       coalesce(sum(case when activo = true then 1 else 0 end), 0) as active
                from "%s".usuarios
                """.formatted(schema),
                (resultSet, rowNumber) -> new TenantUserCounts(
                        resultSet.getLong("total"),
                        resultSet.getLong("active")
                )
        );
    }

    private String validatedSchema(Empresa empresa) {
        String schema = empresa == null ? null : empresa.getSchemaName();
        if (schema == null || !SAFE_SCHEMA.matcher(schema).matches()) {
            throw new BusinessException(
                    "TENANT_SCHEMA_INVALIDO",
                    "El schema de la empresa no es valido para consultar sus usuarios"
            );
        }
        return schema;
    }

    public record TenantUserCounts(long total, long active) {

        public long inactive() {
            return Math.max(total - active, 0);
        }
    }
}
