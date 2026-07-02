package com.azurion.saascore.planes.domain.entities;

import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.shared.domain.BaseEntity;
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
        name = "plan_modulos",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uq_plan_modulo", columnNames = {"plan_id", "modulo_id"})
)
public class PlanModulo extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modulo_id", nullable = false)
    private Modulo modulo;
}
