package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record WhatsappAutoReplyScheduleRequest(
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotNull Boolean activo
) {
}
