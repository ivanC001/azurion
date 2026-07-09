package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RepartirCrmProspectosRequest(
        @NotEmpty List<Long> prospectoIds,
        @NotEmpty List<@NotBlank String> responsableIds,
        Boolean soloNuevos
) {
}
