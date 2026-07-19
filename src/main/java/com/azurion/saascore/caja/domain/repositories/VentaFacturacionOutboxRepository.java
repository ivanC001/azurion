package com.azurion.saascore.caja.domain.repositories;

import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface VentaFacturacionOutboxRepository extends JpaRepository<VentaFacturacionOutbox, Long> {

    List<VentaFacturacionOutbox> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<String> statuses,
            LocalDateTime now
    );

    Optional<VentaFacturacionOutbox> findByIdAndStatusAndLeaseOwner(Long id, String status, String leaseOwner);

    @Modifying
    @Transactional
    @Query("""
            update VentaFacturacionOutbox job
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
            update VentaFacturacionOutbox job
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
            update VentaFacturacionOutbox job
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
            update VentaFacturacionOutbox job
               set job.status = 'COMPLETED',
                   job.lastError = null,
                   job.leaseOwner = null,
                   job.leaseUntil = null,
                   job.heartbeatAt = null,
                   job.updatedAt = :now
             where job.id = :id
               and job.status = 'PROCESSING'
               and job.leaseOwner = :owner
            """)
    int markCompleted(@Param("id") Long id,
                      @Param("owner") String owner,
                      @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            update VentaFacturacionOutbox job
               set job.status = :status,
                   job.nextAttemptAt = :nextAttemptAt,
                   job.lastError = :lastError,
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

    @Modifying
    @Transactional
    @Query("delete from VentaFacturacionOutbox job where job.status = 'COMPLETED' and job.updatedAt < :before")
    int deleteCompletedBefore(@Param("before") LocalDateTime before);
}
