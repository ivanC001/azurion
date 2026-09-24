package com.azurion.saascore.crm.application.dto;

import java.util.List;

/**
 * Resultado de la prueba de avisos: a quien llego y, si fallo, por que.
 */
public record CrmLeadNotificationTestResponse(
        boolean enviado,
        List<String> destinatarios,
        String detalle
) {
}
