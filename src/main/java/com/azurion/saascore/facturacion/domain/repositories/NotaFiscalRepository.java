package com.azurion.saascore.facturacion.domain.repositories;

import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {

    Optional<NotaFiscal> findByExternalId(String externalId);
}
