package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_catalogo_items")
public class CrmCatalogoItem extends BaseEntity {

    @Column(name = "tipo_item", nullable = false, length = 30)
    private String tipoItem;

    @Column(name = "nombre", nullable = false, length = 220)
    private String nombre;

    @Column(name = "descripcion", length = 1500)
    private String descripcion;

    @Column(name = "precio_referencial", nullable = false, precision = 18, scale = 2)
    private BigDecimal precioReferencial = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "ACTIVO";

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "public_token", nullable = false, length = 80)
    private String publicToken;

    @Column(name = "public_enabled", nullable = false)
    private boolean publicEnabled = true;

    @Column(name = "landing_slug", length = 140)
    private String landingSlug;
}
