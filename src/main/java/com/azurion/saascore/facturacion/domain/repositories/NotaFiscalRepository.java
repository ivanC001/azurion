package com.azurion.saascore.facturacion.domain.repositories;

import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {

    Optional<NotaFiscal> findByExternalId(String externalId);

    @Query("""
            select nota from NotaFiscal nota
             where nota.tipoDocumento = :tipo
               and (:query = ''
                    or lower(nota.externalId) like lower(concat('%', :query, '%'))
                    or lower(nota.ventaExternalId) like lower(concat('%', :query, '%'))
                    or lower(coalesce(nota.ventaNumeroDocumento, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(nota.clienteNombre, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(nota.clienteDocumento, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(nota.motivoDescripcion, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(nota.facturacionEstado, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(nota.facturadorSunatEstado, '')) like lower(concat('%', :query, '%')))
            """)
    Page<NotaFiscal> search(@Param("tipo") String tipo,
                            @Param("query") String query,
                            Pageable pageable);
}
