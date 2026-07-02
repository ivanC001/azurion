package com.azurion.saascore.inventory.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateCompraRequest(
        Long proveedorId,
        String proveedorDocumento,
        String proveedorNombre,
        @NotBlank String tipoComprobante,
        String serie,
        String correlativo,
        @NotBlank String numeroComprobante,
        @NotNull LocalDate fechaEmision,
        OffsetDateTime fechaIngreso,
        @NotNull Long almacenId,
        @NotEmpty List<@Valid CompraDetalleRequest> detalles
) {
}
