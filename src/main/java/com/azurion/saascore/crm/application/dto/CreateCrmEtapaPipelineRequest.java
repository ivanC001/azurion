package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCrmEtapaPipelineRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 300) String descripcion,
        @Min(1) Integer orden,
        @Min(0) Integer probabilidadDefault,
        @Size(max = 20) String color,
        @Size(max = 80) String icono,
        Boolean ganado,
        Boolean perdido,
        Boolean requiereValidacion,
        @Size(max = 20) String modoValidacion,
        Boolean activo
) {
}
