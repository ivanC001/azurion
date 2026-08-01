package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmWhatsappMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CrmWhatsappMessageRepository extends JpaRepository<CrmWhatsappMessage, Long> {

    Optional<CrmWhatsappMessage> findByMetaMessageId(String metaMessageId);

    boolean existsByMetaMessageId(String metaMessageId);

    List<CrmWhatsappMessage> findAllByProspecto_IdOrderByMensajeEnAscIdAsc(Long prospectoId);

    @EntityGraph(attributePaths = {"prospecto"})
    List<CrmWhatsappMessage> findAllByProspecto_IdOrderByMensajeEnDescIdDesc(
            Long prospectoId,
            Pageable pageable
    );

    Optional<CrmWhatsappMessage> findFirstByProspecto_IdAndDireccionOrderByMensajeEnDescIdDesc(
            Long prospectoId,
            String direccion
    );

    List<CrmWhatsappMessage> findAllByProspecto_IdAndDireccionAndLeidoEnIsNull(Long prospectoId, String direccion);
}
