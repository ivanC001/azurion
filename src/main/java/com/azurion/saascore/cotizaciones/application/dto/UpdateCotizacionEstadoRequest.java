package com.azurion.saascore.cotizaciones.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record UpdateCotizacionEstadoRequest(
        @NotBlank String estado,
        String canalEnvio,
        OffsetDateTime proximoSeguimientoEn,
        String motivoRechazo,
        String decisionSiguiente
) {
}
