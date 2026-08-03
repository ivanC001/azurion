package com.azurion.saascore.facturacion.domain.repositories;

import com.azurion.saascore.facturacion.domain.entities.NotaFiscal;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {

    Optional<NotaFiscal> findByExternalId(String externalId);

    Optional<NotaFiscal> findByClientOperationId(String clientOperationId);

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

    @Query("""
            select coalesce(sum(nota.monto), 0)
              from NotaFiscal nota
             where nota.tipoDocumento = :tipo
               and nota.fechaEmision >= :from
               and nota.fechaEmision <= :to
               and coalesce(nota.facturadorSunatEstado, nota.facturacionEstado) = 'ACEPTADO'
            """)
    BigDecimal sumAcceptedGrossBetween(@Param("tipo") String tipo,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(nota.baseImponible), 0)
              from NotaFiscal nota
             where nota.tipoDocumento = :tipo
               and nota.fechaEmision >= :from
               and nota.fechaEmision <= :to
               and coalesce(nota.facturadorSunatEstado, nota.facturacionEstado) = 'ACEPTADO'
            """)
    BigDecimal sumAcceptedBaseBetween(@Param("tipo") String tipo,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(nota.montoIgv), 0)
              from NotaFiscal nota
             where nota.tipoDocumento = :tipo
               and nota.fechaEmision >= :from
               and nota.fechaEmision <= :to
               and coalesce(nota.facturadorSunatEstado, nota.facturacionEstado) = 'ACEPTADO'
            """)
    BigDecimal sumAcceptedTaxBetween(@Param("tipo") String tipo,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    @Query("""
            select count(nota)
              from NotaFiscal nota
             where nota.fechaEmision >= :from
               and nota.fechaEmision <= :to
               and coalesce(nota.facturadorSunatEstado, nota.facturacionEstado) = 'ACEPTADO'
               and (nota.baseImponible is null or nota.montoIgv is null)
            """)
    long countAcceptedMissingTaxBreakdown(@Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    @Query("""
            select count(nota)
              from NotaFiscal nota
             where nota.tipoDocumento = '07'
               and nota.fechaEmision >= :from
               and nota.fechaEmision <= :to
               and coalesce(nota.facturadorSunatEstado, nota.facturacionEstado) = 'ACEPTADO'
            """)
    long countAcceptedCreditNotesBetween(@Param("from") LocalDate from,
                                         @Param("to") LocalDate to);
}
