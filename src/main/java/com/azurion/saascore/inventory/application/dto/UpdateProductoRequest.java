package com.azurion.saascore.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateProductoRequest(
        @NotBlank String nombre,
        BigDecimal precio,
        @NotNull Boolean activo,
        String codigo,
        String sku,
        String codigoBarras,
        String descripcion,
        Long categoriaId,
        Long marcaId,
        Long unidadMedidaId,
        String tipoProducto,
        String imagenUrl,
        String foto,
        BigDecimal precioCompraBase,
        BigDecimal precioVentaBase,
        BigDecimal costoPromedio,
        Boolean afectoIgv,
        String tipoAfectacionIgvId,
        String tributoId,
        BigDecimal porcentajeImpuesto,
        Boolean usaConfiguracionEmpresa,
        Boolean manejaStock,
        Boolean manejaLotes,
        Boolean manejaVencimiento,
        Boolean stock,
        Boolean lotes,
        Boolean vencimiento,
        BigDecimal stockMinimoGlobal,
        BigDecimal stockMinimo,
        String estado
) {
}
