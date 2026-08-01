package com.azurion.saascore.clientes.domain.repositories;

import com.azurion.saascore.clientes.domain.entities.Cliente;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cliente from Cliente cliente where cliente.id = :id")
    Optional<Cliente> findByIdForUpdate(@Param("id") Long id);
    Optional<Cliente> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
    boolean existsByTipoDocumentoAndNumeroDocumentoAndIdNot(String tipoDocumento, String numeroDocumento, Long id);

    @Query("""
            select cliente from Cliente cliente
             where :query = ''
                or lower(cliente.nombre) like lower(concat('%', :query, '%'))
                or lower(coalesce(cliente.numeroDocumento, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(cliente.email, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(cliente.telefono, '')) like lower(concat('%', :query, '%'))
            """)
    Page<Cliente> search(@Param("query") String query, Pageable pageable);
}
