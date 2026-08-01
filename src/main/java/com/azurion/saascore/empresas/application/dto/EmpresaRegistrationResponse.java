package com.azurion.saascore.empresas.application.dto;

import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;

public record EmpresaRegistrationResponse(
        EmpresaResponse empresa,
        SuscripcionResponse suscripcion
) {
}
