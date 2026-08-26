package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CrmMetaResponse(
        Long id,
        Integer anio,
        Integer mes,
        String alcance,
        String responsableId,
        String responsableNombre,
        String moneda,
        BigDecimal metaIngresos,
        Integer metaOportunidadesGanadas,
        Integer metaProspectosNuevos,
        Integer metaActividadesRealizadas,
        BigDecimal metaConversion,
        BigDecimal actualIngresos,
        long actualOportunidadesGanadas,
        long actualProspectosNuevos,
        long actualActividadesRealizadas,
        BigDecimal actualConversion,
        int progresoIngresos,
        int progresoOportunidadesGanadas,
        int progresoProspectosNuevos,
        int progresoActividadesRealizadas,
        int progresoConversion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
