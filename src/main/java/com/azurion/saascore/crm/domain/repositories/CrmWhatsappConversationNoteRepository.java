package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversationNote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmWhatsappConversationNoteRepository extends JpaRepository<CrmWhatsappConversationNote, Long> {

    List<CrmWhatsappConversationNote> findAllByConversation_IdOrderBySlotAsc(Long conversationId);

    Optional<CrmWhatsappConversationNote> findByIdAndConversation_Id(Long id, Long conversationId);
}
