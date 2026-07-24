package com.azurion.saascore.suscripciones.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateEmpresaSubscriptionPlanRequest(
        @NotNull Long planId,
        @Min(1) Integer limiteUsuarios
) {
}
