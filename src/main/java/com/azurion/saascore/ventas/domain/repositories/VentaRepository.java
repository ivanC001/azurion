package com.azurion.saascore.ventas.domain.repositories;

import com.azurion.saascore.ventas.domain.entities.Venta;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import com.azurion.saascore.ventas.application.dto.VentaSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByExternalId(String externalId);

    Optional<Venta> findByClientOperationId(String clientOperationId);

    List<Venta> findAllByOrderByFechaVentaDesc();

    @Query("""
            select venta from Venta venta
             where (:query = ''
                    or lower(venta.externalId) like lower(concat('%', :query, '%'))
                    or lower(venta.clienteNombre) like lower(concat('%', :query, '%'))
                    or lower(venta.clienteDocumento) like lower(concat('%', :query, '%'))
                    or lower(venta.moneda) like lower(concat('%', :query, '%'))
                    or lower(coalesce(venta.facturacionEstado, '')) like lower(concat('%', :query, '%'))
                    or lower(coalesce(venta.facturadorSunatEstado, '')) like lower(concat('%', :query, '%')))
            """)
    Page<Venta> search(@Param("query") String query, Pageable pageable);

    @Query("""
            select new com.azurion.saascore.ventas.application.dto.VentaSummaryResponse(
                count(venta),
                coalesce(sum(venta.total), 0),
                coalesce(sum(case when venta.fechaVenta >= :dayStart and venta.fechaVenta < :dayEnd then 1 else 0 end), 0),
                coalesce(sum(case when coalesce(venta.facturadorSunatEstado, venta.facturacionEstado) = 'ACEPTADO' then 1 else 0 end), 0),
                coalesce(sum(case when venta.facturacionEstado <> 'NO_REQUIERE'
                                  and coalesce(venta.facturadorSunatEstado, venta.facturacionEstado) <> 'ACEPTADO'
                                  then 1 else 0 end), 0),
                coalesce(sum(case when venta.facturacionEstado = 'NO_REQUIERE' then 1 else 0 end), 0)
            )
            from Venta venta
            """)
    VentaSummaryResponse summarize(@Param("dayStart") OffsetDateTime dayStart,
                                    @Param("dayEnd") OffsetDateTime dayEnd);
}
