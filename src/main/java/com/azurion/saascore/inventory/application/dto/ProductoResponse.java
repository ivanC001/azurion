package com.azurion.saascore.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String codigo,
        String codigoBarras,
        String sku,
        String nombre,
        String descripcion,
        Long categoriaId,
        Long marcaId,
        Long unidadMedidaId,
        String tipoProducto,
        BigDecimal costoPromedio,
        boolean afectoIgv,
        String tipoAfectacionIgvId,
        String tributoId,
        BigDecimal porcentajeImpuesto,
        boolean usaConfiguracionEmpresa,
        boolean stock,
        boolean lotes,
        boolean vencimiento,
        BigDecimal stockMinimo,
        String foto,
        String estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal precio,
        Long almacenId,
        String almacenCodigo,
        String almacenNombre,
        BigDecimal stockCantidad,
        boolean activo,
        String imagenUrl,
        BigDecimal precioCompraBase,
        BigDecimal precioVentaBase,
        boolean manejaStock,
        boolean manejaLotes,
        boolean manejaVencimiento,
        BigDecimal stockMinimoGlobal
) {
}
