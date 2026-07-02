package com.azurion.saascore.crm.application.dto;

import java.time.OffsetDateTime;

public record CrmOportunidadHistorialResponse(
        Long id,
        Long oportunidadId,
        Long etapaOrigenId,
        String etapaOrigenCodigo,
        String etapaOrigenNombre,
        Long etapaDestinoId,
        String etapaDestinoCodigo,
        String etapaDestinoNombre,
        String usuarioId,
        String observacion,
        OffsetDateTime fechaCambio
) {
}
