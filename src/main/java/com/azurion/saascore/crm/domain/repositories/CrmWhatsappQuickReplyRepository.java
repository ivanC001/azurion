package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappQuickReply;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmWhatsappQuickReplyRepository extends JpaRepository<CrmWhatsappQuickReply, Long> {
    List<CrmWhatsappQuickReply> findAllByUsuarioIdOrderBySlotAsc(String usuarioId);
    Optional<CrmWhatsappQuickReply> findByIdAndUsuarioId(Long id, String usuarioId);
}
