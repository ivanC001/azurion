package com.azurion.saascore.crm.application.dto;

import java.time.LocalTime;

public record WhatsappAutoReplyScheduleResponse(
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean activo
) {
}
