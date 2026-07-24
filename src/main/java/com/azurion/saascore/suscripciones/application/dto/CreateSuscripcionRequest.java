package com.azurion.saascore.suscripciones.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;

public record CreateSuscripcionRequest(
        @NotNull Long empresaId,
        @NotNull Long planId,
        LocalDate fechaInicio,
        @Min(1) Integer limiteUsuarios
) {
}
