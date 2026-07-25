package com.azurion.saascore.crm.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PublicCrmLeadRequest(
        @JsonAlias({"Ruc_tenant", "rucTenant", "tenantId"}) @Size(max = 80) String rucTenant,
        @Size(max = 120) String landingKey,
        @Size(max = 20) String tipoPersona,
        @Size(max = 5) String tipoDocumento,
        @Size(max = 20) String numeroDocumento,
        @Size(max = 180) String nombre,
        @Size(max = 220) String empresa,
        @JsonAlias({"email", "emai"}) @Email @Size(max = 180) String correo,
        @Size(max = 40) String telefono,
        @Size(max = 500) String direccion,
        @Size(max = 30) String origen,
        @Size(max = 30) String canalIngreso,
        @Size(max = 120) String campania,
        @Size(max = 500) String landingUrl,
        @Size(max = 1500) String mensaje,
        @Size(max = 30) String tipoInteres,
        @Size(max = 220) String interesPrincipal,
        @Size(max = 1500) String interesDetalle,
        @Digits(integer = 16, fraction = 2) BigDecimal presupuestoEstimado,
        LocalDate fechaInteres,
        Long catalogoItemId,
        @Size(max = 120) String catalogoToken,
        @Size(max = 120) String website,
        @Size(max = 10000) String metadataJson
) {
    public PublicCrmLeadRequest forIngress(String tenantId, String sourceKey) {
        return new PublicCrmLeadRequest(
                tenantId,
                sourceKey,
                tipoPersona,
                tipoDocumento,
                numeroDocumento,
                nombre,
                empresa,
                correo,
                telefono,
                direccion,
                origen,
                canalIngreso,
                campania,
                landingUrl,
                mensaje,
                tipoInteres,
                interesPrincipal,
                interesDetalle,
                presupuestoEstimado,
                fechaInteres,
                catalogoItemId,
                catalogoToken,
                website,
                metadataJson
        );
    }
}
