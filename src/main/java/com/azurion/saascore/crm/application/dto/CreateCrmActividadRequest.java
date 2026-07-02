package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateCrmActividadRequest(
        Long prospectoId,
        Long oportunidadId,
        Long clienteId,
        @NotBlank String tipoActividad,
        @NotBlank @Size(max = 220) String asunto,
        @Size(max = 1000) String descripcion,
        @NotNull OffsetDateTime fechaProgramada,
        String usuarioId
) {
}
