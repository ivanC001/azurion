package com.azurion.saascore.caja.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GuardarCajaFisicaRequest(
        @NotNull Long sucursalId,
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(min = 3, max = 3) String moneda,
        @NotBlank @Size(max = 20) String estado,
        List<Long> usuarioIds
) {
}
