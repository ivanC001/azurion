package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCrmOportunidadEtapaRequest(
        @NotNull Long etapaId,
        @Size(max = 500) String observacion
) {
}
