package com.azurion.saascore.usuarios.domain.repositories;

import com.azurion.saascore.usuarios.domain.entities.UsuarioTenantRol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioTenantRolRepository extends JpaRepository<UsuarioTenantRol, Long> {

    boolean existsByUsuarioGlobalIdAndTenantIdIgnoreCaseAndActivoTrue(Long usuarioGlobalId, String tenantId);

    List<UsuarioTenantRol> findByUsuarioGlobalIdAndTenantIdIgnoreCaseAndActivoTrue(Long usuarioGlobalId, String tenantId);

    Optional<UsuarioTenantRol> findByUsuarioGlobalIdAndTenantIdIgnoreCaseAndRolCodigoIgnoreCaseAndActivoTrue(
            Long usuarioGlobalId,
            String tenantId,
            String rolCodigo
    );
}
