package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CrmWhatsappConversationRepository extends JpaRepository<CrmWhatsappConversation, Long> {

    Optional<CrmWhatsappConversation> findByProspecto_Id(Long prospectoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select conversation from CrmWhatsappConversation conversation where conversation.prospecto.id = :prospectoId")
    Optional<CrmWhatsappConversation> findForUpdateByProspectoId(@Param("prospectoId") Long prospectoId);

    List<CrmWhatsappConversation> findAllByOrderByUltimoMensajeEnDescIdDesc();

    @EntityGraph(attributePaths = {"prospecto"})
    @Query("""
            select conversation
            from CrmWhatsappConversation conversation
            join conversation.prospecto prospecto
            where (:estado is null or conversation.estado = :estado)
              and (:soloNoLeidas = false or conversation.noLeidos > 0)
              and (:responsableId is null or conversation.responsableId = :responsableId)
              and (:query is null
                   or lower(coalesce(prospecto.nombre, '')) like concat('%', :query, '%')
                   or lower(coalesce(prospecto.telefono, '')) like concat('%', :query, '%')
                   or lower(coalesce(prospecto.correo, '')) like concat('%', :query, '%')
                   or lower(coalesce(prospecto.campania, '')) like concat('%', :query, '%')
                   or lower(coalesce(prospecto.interesPrincipal, '')) like concat('%', :query, '%')
                   or lower(coalesce(conversation.ultimoMensaje, '')) like concat('%', :query, '%'))
            order by conversation.ultimoMensajeEn desc, conversation.id desc
            """)
    List<CrmWhatsappConversation> searchRecent(
            @Param("query") String query,
            @Param("estado") String estado,
            @Param("soloNoLeidas") boolean soloNoLeidas,
            @Param("responsableId") String responsableId,
            Pageable pageable
    );

    long countByNoLeidosGreaterThan(Integer minimum);

    Optional<CrmWhatsappConversation> findFirstByNoLeidosGreaterThanOrderByUltimoMensajeEnDescIdDesc(Integer minimum);

    @Query("select coalesce(sum(conversation.noLeidos), 0) from CrmWhatsappConversation conversation where conversation.noLeidos > 0")
    Long sumUnreadMessages();
}
