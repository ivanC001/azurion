package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplyDispatch;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrmWhatsappAutoReplyDispatchRepository extends JpaRepository<CrmWhatsappAutoReplyDispatch, Long> {
    boolean existsByIncomingMessage_Id(Long incomingMessageId);

    boolean existsByProspecto_IdAndEstadoInAndIdNot(Long prospectoId, Set<String> estados, Long id);

    @EntityGraph(attributePaths = {"incomingMessage", "prospecto"})
    Optional<CrmWhatsappAutoReplyDispatch> findWithDetailsById(Long id);
}
