package com.azurion.saascore.crm.application.dto;

import com.azurion.saascore.crm.domain.entities.LandingProductMode;
import java.time.LocalDateTime;
import java.util.List;

public record CrmLandingConfigResponse(
        Long id,
        String nombre,
        String landingKey,
        String campania,
        String canalIngreso,
        Boolean activa,
        Boolean recibirLeads,
        LandingProductMode modoProducto,
        Boolean crearActividadInicial,
        String responsableId,
        List<Long> catalogoItemIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
