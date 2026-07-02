package com.azurion.saascore.usuarios.domain.repositories;

import com.azurion.saascore.usuarios.domain.entities.UsuarioGlobalRol;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioGlobalRolRepository extends JpaRepository<UsuarioGlobalRol, Long> {

    List<UsuarioGlobalRol> findByUsuarioGlobalIdAndActivoTrue(Long usuarioGlobalId);
}
