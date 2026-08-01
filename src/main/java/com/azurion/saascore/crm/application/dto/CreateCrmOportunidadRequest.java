package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CreateCrmOportunidadRequest(
        Long prospectoId,
        Long clienteId,
        String tipoOportunidad,
        @NotNull Long catalogoItemId,
        @NotBlank @Size(max = 220) String titulo,
        @Size(max = 1000) String descripcion,
        @NotNull @DecimalMin(value = "0.01") BigDecimal montoEstimado,
        @NotNull @Min(0) @Max(100) Integer probabilidad,
        String etapa,
        @NotNull LocalDate fechaCierreEstimada,
        @NotBlank @Size(max = 80) String responsableId,
        @NotBlank @Size(max = 220) String proximaAccion,
        @NotNull @Future OffsetDateTime fechaProximaAccion
) {
}
