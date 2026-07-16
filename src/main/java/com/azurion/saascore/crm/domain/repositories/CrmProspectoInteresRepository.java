package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmProspectoInteres;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrmProspectoInteresRepository extends JpaRepository<CrmProspectoInteres, Long> {

    List<CrmProspectoInteres> findByProspectoIdOrderByUltimoEnvioEnDescIdDesc(Long prospectoId);

    @Query("""
            select interes
            from CrmProspectoInteres interes
            where interes.prospecto.id = :prospectoId
              and coalesce(interes.landingKey, '') = coalesce(:landingKey, '')
              and coalesce(interes.campania, '') = coalesce(:campania, '')
              and ((interes.catalogoItemId is null and :catalogoItemId is null) or interes.catalogoItemId = :catalogoItemId)
              and interes.productoPendiente = :productoPendiente
            order by interes.ultimoEnvioEn desc, interes.id desc
            """)
    List<CrmProspectoInteres> findMatchingPublicLeadInterest(
            @Param("prospectoId") Long prospectoId,
            @Param("landingKey") String landingKey,
            @Param("campania") String campania,
            @Param("catalogoItemId") Long catalogoItemId,
            @Param("productoPendiente") boolean productoPendiente
    );
}
