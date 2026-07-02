package com.azurion.saascore.usuarios.domain.repositories;

import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioTenantRepository extends JpaRepository<UsuarioTenant, Long> {

    Optional<UsuarioTenant> findByUsernameAndActivoTrue(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"usuarioRoles", "usuarioRoles.rol"})
    List<UsuarioTenant> findAllByOrderByNombresAsc();

    @EntityGraph(attributePaths = {"usuarioRoles", "usuarioRoles.rol"})
    Optional<UsuarioTenant> findWithUsuarioRolesById(Long id);

    @EntityGraph(attributePaths = {"usuarioRoles", "usuarioRoles.rol"})
    Optional<UsuarioTenant> findWithUsuarioRolesByUsernameIgnoreCase(String username);
}
