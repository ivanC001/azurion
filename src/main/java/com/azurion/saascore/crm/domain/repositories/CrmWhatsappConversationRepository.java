package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrmWhatsappConversationRepository extends JpaRepository<CrmWhatsappConversation, Long> {

    Optional<CrmWhatsappConversation> findByProspecto_Id(Long prospectoId);

    List<CrmWhatsappConversation> findAllByOrderByUltimoMensajeEnDescIdDesc();

    long countByNoLeidosGreaterThan(Integer minimum);

    Optional<CrmWhatsappConversation> findFirstByNoLeidosGreaterThanOrderByUltimoMensajeEnDescIdDesc(Integer minimum);

    @Query("select coalesce(sum(conversation.noLeidos), 0) from CrmWhatsappConversation conversation where conversation.noLeidos > 0")
    Long sumUnreadMessages();
}
