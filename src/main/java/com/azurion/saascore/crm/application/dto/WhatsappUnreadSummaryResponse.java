package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

public record WhatsappUnreadSummaryResponse(
        long mensajesNoLeidos,
        long conversacionesNoLeidas,
        Long ultimoProspectoId,
        String ultimoContacto,
        String ultimoMensaje,
        OffsetDateTime ultimoMensajeEn
) {
}
