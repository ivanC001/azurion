package com.azurion.saascore.clientes.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegistrarClienteAbonoRequest(
        @NotNull(message = "El monto del abono es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto del abono debe ser mayor a cero")
        BigDecimal monto,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion
) {
}
