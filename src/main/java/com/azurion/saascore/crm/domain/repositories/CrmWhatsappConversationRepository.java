package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmWhatsappConversationRepository extends JpaRepository<CrmWhatsappConversation, Long> {

    Optional<CrmWhatsappConversation> findByProspecto_Id(Long prospectoId);

    List<CrmWhatsappConversation> findAllByOrderByUltimoMensajeEnDescIdDesc();
}
