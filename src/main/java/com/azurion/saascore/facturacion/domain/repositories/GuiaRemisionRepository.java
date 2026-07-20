package com.azurion.saascore.facturacion.domain.repositories;

import com.azurion.saascore.facturacion.domain.entities.GuiaRemision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuiaRemisionRepository extends JpaRepository<GuiaRemision, Long> {
    Optional<GuiaRemision> findByExternalId(String externalId);

    @Query("""
            select guia from GuiaRemision guia
             where :query = ''
                or lower(guia.externalId) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.sucursalOrigenNombre, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.sucursalDestinoNombre, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.motivoTraslado, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.transportista, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.responsableNombre, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.facturacionEstado, '')) like lower(concat('%', :query, '%'))
                or lower(coalesce(guia.facturadorSunatEstado, '')) like lower(concat('%', :query, '%'))
            """)
    Page<GuiaRemision> search(@Param("query") String query, Pageable pageable);
}
