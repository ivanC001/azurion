package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "crm_landing_catalog_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_crm_landing_catalog_item", columnNames = {"landing_config_id", "catalogo_item_id"})
)
public class CrmLandingCatalogItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landing_config_id", nullable = false)
    private CrmLandingConfig landingConfig;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalogo_item_id", nullable = false)
    private CrmCatalogoItem catalogoItem;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;
}
