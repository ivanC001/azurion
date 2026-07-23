package com.azurion.saascore.crm.application.dto;

import com.azurion.saascore.crm.domain.entities.LandingProductMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveCrmLandingConfigRequest(
        @NotBlank @Size(max = 160) String nombre,
        @Size(max = 120) String campania,
        LandingProductMode modoProducto,
        Boolean activa,
        Boolean recibirLeads,
        Boolean crearActividadInicial,
        @Size(max = 80) String responsableId,
        List<Long> catalogoItemIds
) {
}
