package com.azurion.saascore.caja.domain.repositories;

import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

    @Modifying
    @Transactional
    @Query("""
            update VentaFacturacionOutbox job
               set job.status = 'PROCESSING',
                   job.attempts = job.attempts + 1,
                   job.updatedAt = :now
             where job.id = :id
               and job.status in ('PENDING', 'RETRY')
               and job.nextAttemptAt <= :now
            """)
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            update VentaFacturacionOutbox job
               set job.status = 'RETRY',
                   job.nextAttemptAt = :now,
                   job.lastError = 'Tarea recuperada despues de una interrupcion',
                   job.updatedAt = :now
             where job.status = 'PROCESSING'
               and job.updatedAt < :staleBefore
            """)
    int recoverStuck(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            update VentaFacturacionOutbox job
               set job.status = 'COMPLETED',
                   job.lastError = null,
                   job.updatedAt = :now
             where job.id = :id and job.status = 'PROCESSING'
            """)
    int markCompleted(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
            update VentaFacturacionOutbox job
               set job.status = :status,
                   job.nextAttemptAt = :nextAttemptAt,
                   job.lastError = :lastError,
                   job.updatedAt = :now
             where job.id = :id and job.status = 'PROCESSING'
            """)
    int markFailedAttempt(@Param("id") Long id,
                          @Param("status") String status,
                          @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
                          @Param("lastError") String lastError,
                          @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("delete from VentaFacturacionOutbox job where job.status = 'COMPLETED' and job.updatedAt < :before")
    int deleteCompletedBefore(@Param("before") LocalDateTime before);
}
