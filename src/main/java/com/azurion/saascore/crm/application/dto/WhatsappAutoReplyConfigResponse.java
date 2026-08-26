package com.azurion.saascore.crm.application.dto;

import java.util.List;

public record WhatsappAutoReplyConfigResponse(
        boolean activo,
        String modo,
        String mensaje,
        Integer cooldownMinutos,
        String zonaHoraria,
        List<WhatsappAutoReplyScheduleResponse> horarios
) {
}
