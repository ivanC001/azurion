package com.azurion.saascore.clientes.domain.repositories;

import com.azurion.saascore.clientes.domain.entities.ClienteAbono;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteAbonoRepository extends JpaRepository<ClienteAbono, Long> {
    List<ClienteAbono> findByClienteIdOrderByCreatedAtDesc(Long clienteId);
    boolean existsByClienteId(Long clienteId);
}
