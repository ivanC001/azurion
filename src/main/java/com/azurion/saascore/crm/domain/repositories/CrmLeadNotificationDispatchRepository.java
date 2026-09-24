package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationDispatch;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmLeadNotificationDispatchRepository extends JpaRepository<CrmLeadNotificationDispatch, Long> {

    /** Freno del cooldown: un aviso reciente del mismo tipo para el mismo prospecto. */
    @Query("""
            SELECT COUNT(d) > 0 FROM CrmLeadNotificationDispatch d
            WHERE d.prospecto.id = :prospectoId
              AND d.tipo = :tipo
              AND d.estado <> 'ERROR'
              AND d.createdAt >= :desde
            """)
    boolean existsRecent(@Param("prospectoId") Long prospectoId,
                         @Param("tipo") String tipo,
                         @Param("desde") LocalDateTime desde);

    @Query("""
            SELECT d FROM CrmLeadNotificationDispatch d
            JOIN FETCH d.prospecto
            ORDER BY d.createdAt DESC, d.id DESC
            """)
    List<CrmLeadNotificationDispatch> findRecent(Pageable pageable);
}
