package com.azurion.saascore.suscripciones.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UpdateSuscripcionEstadoRequest(
        @NotBlank String estado,
        LocalDate fechaFin
) {
}
