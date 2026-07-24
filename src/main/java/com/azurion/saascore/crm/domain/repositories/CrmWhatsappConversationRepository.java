package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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

    long countByNoLeidosGreaterThan(Integer minimum);

    Optional<CrmWhatsappConversation> findFirstByNoLeidosGreaterThanOrderByUltimoMensajeEnDescIdDesc(Integer minimum);

    @Query("select coalesce(sum(conversation.noLeidos), 0) from CrmWhatsappConversation conversation where conversation.noLeidos > 0")
    Long sumUnreadMessages();
}
