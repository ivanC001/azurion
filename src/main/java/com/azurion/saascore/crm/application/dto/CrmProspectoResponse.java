package com.azurion.saascore.crm.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CrmProspectoResponse(
        Long id,
        String tipoPersona,
        String tipoDocumento,
        String numeroDocumento,
        String nombre,
        String razonSocial,
        String nombreComercial,
        String telefono,
        String correo,
        String direccion,
        String origen,
        String canalIngreso,
        String campania,
        String landingUrl,
        String mensaje,
        String tipoInteres,
        String interesPrincipal,
        String interesDetalle,
        BigDecimal presupuestoEstimado,
        LocalDate fechaInteres,
        Long catalogoItemId,
        String metadataJson,
        String estado,
        String nivelInteres,
        String responsableId,
        String observacion,
        Long clienteId,
        OffsetDateTime fechaConversion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
