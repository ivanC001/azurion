package com.azurion.saascore.crm.domain.repositories;

import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CrmCatalogoItemRepository extends JpaRepository<CrmCatalogoItem, Long> {

    List<CrmCatalogoItem> findAllByOrderByIdDesc();

    List<CrmCatalogoItem> findByTipoItemOrderByIdDesc(String tipoItem);

    Optional<CrmCatalogoItem> findByIdAndPublicToken(Long id, String publicToken);

    boolean existsByPublicToken(String publicToken);

    @Query(value = """
            select c.id as "catalogoItemId",
                   coalesce(leads.total, 0) as "prospectosCount",
                   coalesce(opportunities.total, 0) as "oportunidadesCount",
                   coalesce(landings.total, 0) as "landingsCount"
            from crm_catalogo_items c
            left join (
                select catalogo_item_id, count(distinct prospecto_id) as total
                from (
                    select p.catalogo_item_id, p.id as prospecto_id
                    from crm_prospectos p
                    where p.catalogo_item_id is not null
                    union all
                    select i.catalogo_item_id, i.prospecto_id
                    from crm_prospecto_intereses i
                    where i.catalogo_item_id is not null
                ) catalog_leads
                group by catalogo_item_id
            ) leads on leads.catalogo_item_id = c.id
            left join (
                select o.catalogo_item_id, count(*) as total
                from crm_oportunidades o
                where o.catalogo_item_id is not null
                group by o.catalogo_item_id
            ) opportunities on opportunities.catalogo_item_id = c.id
            left join (
                select l.catalogo_item_id, count(*) as total
                from crm_landing_catalog_items l
                join crm_landing_config lc on lc.id = l.landing_config_id
                where l.activo = true and lc.activa = true
                group by l.catalogo_item_id
            ) landings on landings.catalogo_item_id = c.id
            """, nativeQuery = true)
    List<CrmCatalogoUsageProjection> summarizeUsage();

    interface CrmCatalogoUsageProjection {

        Long getCatalogoItemId();

        Long getProspectosCount();

        Long getOportunidadesCount();

        Long getLandingsCount();
    }
}
