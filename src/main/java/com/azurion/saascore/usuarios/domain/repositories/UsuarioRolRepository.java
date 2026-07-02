package com.azurion.saascore.usuarios.domain.repositories;

import com.azurion.saascore.usuarios.domain.entities.UsuarioRol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {

    boolean existsByUsuarioIdAndRolId(Long usuarioId, Long rolId);

    Optional<UsuarioRol> findByUsuarioIdAndRolId(Long usuarioId, Long rolId);

    long countByRolId(Long rolId);

    List<UsuarioRol> findByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
