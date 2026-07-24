package com.azurion.saascore.auth.domain.repositories;

import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioGlobalRepository extends JpaRepository<UsuarioGlobal, Long> {
    Optional<UsuarioGlobal> findByUsernameAndActivoTrue(String username);

    Optional<UsuarioGlobal> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<UsuarioGlobal> findByActivoTrueOrderByUsernameAsc();
}
