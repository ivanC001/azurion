package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateCrmOportunidadRequest(
        Long prospectoId,
        Long clienteId,
        String tipoOportunidad,
        Long catalogoItemId,
        @Size(max = 220) String titulo,
        @Size(max = 1000) String descripcion,
        @DecimalMin("0.00") BigDecimal montoEstimado,
        @Min(0) @Max(100) Integer probabilidad,
        String etapa,
        LocalDate fechaCierreEstimada,
        String responsableId,
        String estado,
        @Size(max = 500) String motivoPerdida
) {
}
