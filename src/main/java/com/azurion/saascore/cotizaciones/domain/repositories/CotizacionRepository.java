package com.azurion.saascore.cotizaciones.domain.repositories;

import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.LockModeType;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    @Query("select distinct quote from Cotizacion quote where quote.id = :id")
    Optional<Cotizacion> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    @Query("select quote from Cotizacion quote where quote.id = :id")
    Optional<Cotizacion> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.whatsappSendStatus = 'SENDING',
                   quote.whatsappSendToken = :token,
                   quote.whatsappSendStartedAt = :now,
                   quote.whatsappSendError = null,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and (quote.whatsappSendStatus is null or quote.whatsappSendStatus = 'ERROR')
            """)
    int claimWhatsappSend(@Param("id") Long id,
                          @Param("token") String token,
                          @Param("now") OffsetDateTime now,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.whatsappSendStatus = 'SENT',
                   quote.whatsappMessageId = :messageId,
                   quote.whatsappSendError = null,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and quote.whatsappSendStatus = 'SENDING'
               and quote.whatsappSendToken = :token
            """)
    int markWhatsappSent(@Param("id") Long id,
                         @Param("token") String token,
                         @Param("messageId") String messageId,
                         @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.whatsappSendStatus = 'ERROR',
                   quote.whatsappSendError = :error,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and quote.whatsappSendStatus = 'SENDING'
               and quote.whatsappSendToken = :token
            """)
    int markWhatsappFailed(@Param("id") Long id,
                           @Param("token") String token,
                           @Param("error") String error,
                           @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying
    @Transactional
    @Query("""
            update Cotizacion quote
               set quote.whatsappSendStatus = 'UNKNOWN',
                   quote.whatsappSendError = :error,
                   quote.updatedAt = :updatedAt
             where quote.id = :id
               and quote.whatsappSendStatus = 'SENDING'
               and quote.whatsappSendToken = :token
            """)
    int markWhatsappUncertain(@Param("id") Long id,
                              @Param("token") String token,
                              @Param("error") String error,
                              @Param("updatedAt") LocalDateTime updatedAt);

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

    @Query("select quote.id from Cotizacion quote order by quote.fechaEmision desc, quote.id desc")
    Page<Long> findRecentIds(Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    @Query("select distinct quote from Cotizacion quote where quote.id in :ids order by quote.fechaEmision desc, quote.id desc")
    List<Cotizacion> findDetailedByIdIn(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    List<Cotizacion> findByCrmOportunidadIdOrderByFechaEmisionDescIdDesc(Long crmOportunidadId);

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    @Query("""
            select quote
              from Cotizacion quote
             where exists (
                    select opportunity.id
                      from CrmOportunidad opportunity
                     where opportunity.id = quote.crmOportunidadId
                       and opportunity.prospecto.id = :prospectoId
             )
             order by quote.fechaEmision desc, quote.id desc
            """)
    List<Cotizacion> findAllByCrmProspectoId(@Param("prospectoId") Long prospectoId);

    @EntityGraph(attributePaths = {"cliente", "sucursal", "detalles", "detalles.producto", "detalles.promocion"})
    @Query("""
            select quote
              from Cotizacion quote
             where quote.id = :quoteId
               and exists (
                    select opportunity.id
                      from CrmOportunidad opportunity
                     where opportunity.id = quote.crmOportunidadId
                       and opportunity.prospecto.id = :prospectoId
             )
            """)
    Optional<Cotizacion> findByIdAndCrmProspectoId(
            @Param("quoteId") Long quoteId,
            @Param("prospectoId") Long prospectoId
    );

    @EntityGraph(attributePaths = {"cliente", "sucursal"})
    @Query(value = """
            select quote
             from Cotizacion quote
              left join quote.cliente client
             where upper(coalesce(quote.canalEnvio, '')) = 'CORREO'
               and upper(coalesce(quote.emailSendStatus, '')) = 'SENT'
               and quote.fechaEnvio is not null
               and (
                    :query = ''
                    or lower(coalesce(client.nombre, '')) like concat('%', :query, '%')
                    or lower(coalesce(client.email, '')) like concat('%', :query, '%')
                    or lower(coalesce(client.numeroDocumento, '')) like concat('%', :query, '%')
                    or lower(coalesce(quote.observacion, '')) like concat('%', :query, '%')
               )
            """,
            countQuery = """
            select count(quote)
             from Cotizacion quote
              left join quote.cliente client
             where upper(coalesce(quote.canalEnvio, '')) = 'CORREO'
               and upper(coalesce(quote.emailSendStatus, '')) = 'SENT'
               and quote.fechaEnvio is not null
               and (
                    :query = ''
                    or lower(coalesce(client.nombre, '')) like concat('%', :query, '%')
                    or lower(coalesce(client.email, '')) like concat('%', :query, '%')
                    or lower(coalesce(client.numeroDocumento, '')) like concat('%', :query, '%')
                    or lower(coalesce(quote.observacion, '')) like concat('%', :query, '%')
               )
            """)
    Page<Cotizacion> findSentEmails(@Param("query") String query, Pageable pageable);
}
