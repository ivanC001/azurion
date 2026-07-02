package com.azurion.saascore.clientes.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateClienteRequest(
        @NotBlank @Pattern(regexp = "^(1|6)$", message = "tipoDocumento must be 1 (DNI) or 6 (RUC)") String tipoDocumento,
        @NotBlank @Pattern(regexp = "^[0-9]{8,11}$") String numeroDocumento,
        @NotBlank String nombre,
        @Email String email,
        @Size(max = 500) String direccion,
        @Pattern(regexp = "^$|^[0-9]{6}$", message = "ubigeo must have 6 digits") String ubigeo,
        @Size(max = 30) String telefono,
        @DecimalMin("0.00") BigDecimal limiteCredito,
        @Min(0) Integer diasCredito,
        Boolean activo
) {
}
