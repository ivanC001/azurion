package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Size;

public record RealizarCrmActividadRequest(
        @Size(max = 1000) String resultado,
        String resultadoContacto,
        String nivelInteres,
        String estadoProspecto
) {
}
