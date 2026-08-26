package com.azurion.saascore.crm.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateWhatsappAutoReplyConfigRequest(
        @NotNull Boolean activo,
        @NotNull @Pattern(regexp = "SIEMPRE|HORARIO") String modo,
        @Size(max = 4096) String mensaje,
        @NotNull @Min(1) @Max(10080) Integer cooldownMinutos,
        @NotNull @Size(max = 7) List<@Valid WhatsappAutoReplyScheduleRequest> horarios
) {
}
