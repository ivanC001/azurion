package com.azurion.saascore.crm.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PublicCrmLeadRequest(
        String tipoPersona,
        String tipoDocumento,
        String numeroDocumento,
        @NotBlank @Size(max = 180) String nombre,
        @Size(max = 220) String empresa,
        @Email @Size(max = 180) String correo,
        @Size(max = 40) String telefono,
        @Size(max = 500) String direccion,
        String origen,
        String canalIngreso,
        @Size(max = 120) String campania,
        @Size(max = 500) String landingUrl,
        @Size(max = 1500) String mensaje,
        String tipoInteres,
        @Size(max = 220) String interesPrincipal,
        @Size(max = 1500) String interesDetalle,
        BigDecimal presupuestoEstimado,
        LocalDate fechaInteres,
        Long catalogoItemId,
        @Size(max = 120) String catalogoToken,
        @Size(max = 120) String website,
        String metadataJson
) {
}
