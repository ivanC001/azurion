package com.azurion.saascore.empresas.domain.repositories;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByTenantId(String tenantId);
    Optional<Empresa> findByRuc(String ruc);
    Optional<Empresa> findByRucIgnoreCase(String ruc);
    Optional<Empresa> findBySchemaName(String schemaName);
}
