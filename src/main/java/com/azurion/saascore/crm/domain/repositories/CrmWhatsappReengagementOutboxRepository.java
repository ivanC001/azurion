package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappReengagementOutbox;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CrmWhatsappReengagementOutboxRepository
        extends JpaRepository<CrmWhatsappReengagementOutbox, Long> {

    List<CrmWhatsappReengagementOutbox> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<String> statuses,
            LocalDateTime now
    );

    Optional<CrmWhatsappReengagementOutbox> findByIdAndStatusAndLeaseOwner(
            Long id,
            String status,
            String leaseOwner
    );

    Optional<CrmWhatsappReengagementOutbox> findByTenantIdAndDedupeKey(String tenantId, String dedupeKey);

    List<CrmWhatsappReengagementOutbox> findAllByTenantIdAndProspectoIdOrderByScheduledAtDesc(
            String tenantId,
            Long prospectoId
    );

    @Modifying
    @Transactional
    @Query("""
            update CrmWhatsappReengagementOutbox job
               set job.status = 'PROCESSING',
                   job.attempts = job.attempts + 1,
                   job.leaseOwner = :owner,
                   job.leaseUntil = :leaseUntil,
                   job.heartbeatAt = :now,
                   job.updatedAt = :now
             where job.id = :id
               and job.status in ('PENDING', 'RETRY')
               and job.nextAttemptAt <= :now
            """)
    int claim(@Param("id") Long id,
              @Param("owner") String owner,
              @Param("now") LocalDateTime now,
              @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Transactional
    @Query("""
            update CrmWhatsappReengagementOutbox job
               set job.leaseUntil = :leaseUntil,
                   job.heartbeatAt = :now,
                   job.updatedAt = :now
             where job.id = :id
               and job.status = 'PROCESSING'
               and job.leaseOwner = :owner
            """)
    int heartbeat(@Param("id") Long id,
                  @Param("owner") String owner,
                  @Param("now") LocalDateTime now,
                  @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Transactional
    @Query("""
            update CrmWhatsappReengagementOutbox job
               set job.status = 'RETRY',
                   job.nextAttemptAt = :now,
                   job.lastError = 'Tarea recuperada despues de una interrupcion',
                   job.leaseOwner = null,
                   job.leaseUntil = null,
                   job.heartbeatAt = null,
                   job.updatedAt = :now
             where job.status = 'PROCESSING'
               and job.leaseUntil < :now
            """)
    int recoverExpiredLeases(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            update CrmWhatsappReengagementOutbox job
               set job.status = :status,
                   job.resultado = :resultado,
                   job.lastError = null,
                   job.processedAt = :now,
                   job.leaseOwner = null,
                   job.leaseUntil = null,
                   job.heartbeatAt = null,
                   job.updatedAt = :now
             where job.id = :id
               and job.status = 'PROCESSING'
               and job.leaseOwner = :owner
            """)
    int markResolved(@Param("id") Long id,
                     @Param("owner") String owner,
                     @Param("status") String status,
                     @Param("resultado") String resultado,
                     @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            update CrmWhatsappReengagementOutbox job
               set job.status = :status,
                   job.nextAttemptAt = :nextAttemptAt,
                   job.lastError = :lastError,
                   job.processedAt = case when :status = 'FAILED' then :now else job.processedAt end,
                   job.leaseOwner = null,
                   job.leaseUntil = null,
                   job.heartbeatAt = null,
                   job.updatedAt = :now
             where job.id = :id
               and job.status = 'PROCESSING'
               and job.leaseOwner = :owner
            """)
    int markFailedAttempt(@Param("id") Long id,
                          @Param("owner") String owner,
                          @Param("status") String status,
                          @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                          @Param("lastError") String lastError,
                          @Param("now") LocalDateTime now);

    /**
     * Cancela lo que aun no salio. Se usa cuando el cliente responde (la ventana se
     * reabre y ya no hace falta gastar una plantilla) o cuando pide la baja.
     */
    @Modifying
    @Transactional
    @Query("""
            update CrmWhatsappReengagementOutbox job
               set job.status = 'CANCELLED',
                   job.resultado = :motivo,
                   job.processedAt = :now,
                   job.leaseOwner = null,
                   job.leaseUntil = null,
                   job.heartbeatAt = null,
                   job.updatedAt = :now
             where job.tenantId = :tenantId
               and job.prospectoId = :prospectoId
               and job.status in ('PENDING', 'RETRY')
            """)
    int cancelPendingForProspecto(@Param("tenantId") String tenantId,
                                  @Param("prospectoId") Long prospectoId,
                                  @Param("motivo") String motivo,
                                  @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            delete from CrmWhatsappReengagementOutbox job
             where job.status in ('SENT', 'SKIPPED', 'CANCELLED')
               and job.updatedAt < :before
            """)
    int deleteResolvedBefore(@Param("before") LocalDateTime before);
}
