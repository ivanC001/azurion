package com.azurion.saascore.inventory.application.mappers;

import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.domain.entities.Producto;
import java.math.BigDecimal;

public final class ProductoInventoryMapper {

    private ProductoInventoryMapper() {
    }

    public static ProductoResponse toResponse(Producto producto, BigDecimal stockCantidad) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getCodigoBarras(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria() == null ? null : producto.getCategoria().getId(),
                producto.getMarca() == null ? null : producto.getMarca().getId(),
                producto.getUnidadMedida() == null ? null : producto.getUnidadMedida().getId(),
                producto.getTipoProducto(),
                producto.getCostoPromedio(),
                producto.isAfectoIgv(),
                producto.getTipoAfectacionIgvId(),
                producto.getTributoId(),
                producto.getPorcentajeImpuesto(),
                producto.isUsaConfiguracionEmpresa(),
                producto.isManejaStock(),
                producto.isManejaLotes(),
                producto.isManejaVencimiento(),
                producto.getStockMinimo(),
                producto.getFoto(),
                producto.getEstado(),
                producto.getCreatedAt(),
                producto.getUpdatedAt(),
                producto.getPrecio(),
                producto.getAlmacen().getId(),
                producto.getAlmacen().getCodigo(),
                producto.getAlmacen().getNombre(),
                stockCantidad == null ? BigDecimal.ZERO : stockCantidad,
                producto.isActivo(),
                producto.getImagenUrl(),
                producto.getPrecioCompraBase(),
                producto.getPrecioVentaBase(),
                producto.isManejaStock(),
                producto.isManejaLotes(),
                producto.isManejaVencimiento(),
                producto.getStockMinimoGlobal()
        );
    }
}
