package com.azurion.saascore.inventory.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductoRapidoRequest(
        @Size(max = 80) String codigoBarras,
        @Size(max = 80) String sku,
        @NotBlank @Size(max = 255) String nombre,
        @Size(max = 500) String descripcion,
        Long categoriaId,
        Long unidadMedidaId,
        String tipoProducto,
        @jakarta.validation.constraints.NotNull @DecimalMin("0.00") BigDecimal precioVenta,
        @DecimalMin("0.00") BigDecimal costoInicial,
        @DecimalMin("0.00") BigDecimal cantidadInicial,
        Long almacenId,
        Boolean manejaVencimiento,
        @DecimalMin("0.00") BigDecimal stockMinimo,
        @Size(max = 100) String codigoLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        String foto
) {
}
