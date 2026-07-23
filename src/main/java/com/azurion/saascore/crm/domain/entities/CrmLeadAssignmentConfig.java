package com.azurion.saascore.crm.domain.entities;

import com.azurion.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crm_lead_assignment_config")
public class CrmLeadAssignmentConfig extends BaseEntity {

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo = "DEFAULT";

    @Column(name = "automatico", nullable = false)
    private boolean automatico;

    @Column(name = "estrategia", nullable = false, length = 30)
    private String estrategia = "MENOR_CARGA";

    @Column(name = "responsable_ids", columnDefinition = "TEXT")
    private String responsableIds;
}
