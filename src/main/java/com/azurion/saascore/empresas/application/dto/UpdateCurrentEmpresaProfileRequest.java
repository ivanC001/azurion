package com.azurion.saascore.empresas.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCurrentEmpresaProfileRequest(
        @NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._/-]{2,39}$") String ruc,
        @NotBlank @Size(max = 255) String razonSocial,
        @NotBlank @Size(max = 30) String tipoDocumentoFiscal,
        @Size(max = 180) String nombreComercial,
        @Size(max = 500) String direccionFiscal,
        @Size(max = 120) String distrito,
        @Size(max = 120) String provincia,
        @Size(max = 120) String departamento,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String paisCodigo,
        @NotBlank @Size(max = 100) String paisNombre,
        @Email @Size(max = 180) String correoPrincipal,
        @Size(max = 40) String telefono,
        @Size(max = 40) String celular,
        @Size(max = 300) String sitioWeb,
        @Size(max = 300) String facebook,
        @Size(max = 300) String instagram,
        @Size(max = 180) String representanteNombre,
        @Size(max = 30) String representanteTipoDocumento,
        @Size(max = 40) String representanteNumeroDocumento,
        @Size(max = 120) String representanteCargo,
        @Email @Size(max = 180) String representanteCorreo,
        @Size(max = 40) String representanteTelefono,
        @NotBlank @Size(max = 80) String zonaHoraria,
        @NotBlank @Size(max = 20) String idioma,
        @NotBlank @Pattern(regexp = "^(DD/MM/YYYY|MM/DD/YYYY|YYYY-MM-DD)$") String formatoFecha,
        @NotBlank @Pattern(regexp = "^(12H|24H)$") String formatoHora,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{3}$") String monedaCodigo,
        @NotBlank @Size(max = 10) String monedaSimbolo
) {
}
