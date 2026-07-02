package com.azurion.saascore.tributacion.domain.repositories;

import com.azurion.saascore.tributacion.domain.entities.ConfiguracionTributariaEmpresa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionTributariaEmpresaRepository extends JpaRepository<ConfiguracionTributariaEmpresa, Long> {
    Optional<ConfiguracionTributariaEmpresa> findFirstByOrderByIdAsc();
}
