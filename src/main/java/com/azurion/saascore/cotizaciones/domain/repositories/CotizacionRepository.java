package com.azurion.saascore.cotizaciones.domain.repositories;

import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import java.util.List;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.emailSendStatus = 'SENDING',
                   quote.emailSendToken = :token,
                   quote.emailSendStartedAt = :now,
                   quote.emailSendError = null,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and (quote.emailSendStatus is null or quote.emailSendStatus = 'ERROR')
            """)
    int claimEmailSend(@Param("id") Long id,
                       @Param("token") String token,
                       @Param("now") OffsetDateTime now,
                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.emailSendStatus = 'SENT',
                   quote.emailSendError = null,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and quote.emailSendStatus = 'SENDING'
               and quote.emailSendToken = :token
            """)
    int markEmailSent(@Param("id") Long id,
                      @Param("token") String token,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.emailSendStatus = 'ERROR',
                   quote.emailSendError = :error,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and quote.emailSendStatus = 'SENDING'
               and quote.emailSendToken = :token
            """)
    int markEmailFailed(@Param("id") Long id,
                        @Param("token") String token,
                        @Param("error") String error,
                        @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.emailSendStatus = 'UNKNOWN',
                   quote.emailSendError = :error,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and quote.emailSendStatus = 'SENDING'
               and quote.emailSendToken = :token
            """)
    int markEmailUncertain(@Param("id") Long id,
                           @Param("token") String token,
                           @Param("error") String error,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    List<Cotizacion> findAllByOrderByFechaEmisionDescIdDesc();

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    List<Cotizacion> findByCrmOportunidadIdOrderByFechaEmisionDescIdDesc(Long crmOportunidadId);
}
