package com.azurion.saascore.facturacion.domain.repositories;

import com.azurion.saascore.facturacion.domain.entities.FacturadorCallbackNonce;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface FacturadorCallbackNonceRepository extends JpaRepository<FacturadorCallbackNonce, Long> {

    @Modifying
    @Transactional
    @Query("delete from FacturadorCallbackNonce nonce where nonce.expiresAt < :now")
    int deleteExpired(Instant now);
}
