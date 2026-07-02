package com.azurion.saascore.inventory.application.mappers;

import com.azurion.saascore.inventory.application.dto.CompraDetalleResponse;
import com.azurion.saascore.inventory.application.dto.CompraResponse;
import com.azurion.saascore.inventory.domain.entities.Compra;
import com.azurion.saascore.inventory.domain.entities.CompraDetalle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class CompraInventoryMapper {

    private CompraInventoryMapper() {
    }

    public static CompraResponse toResponse(Compra compra, List<CompraDetalle> detalles) {
        BigDecimal ventaProyectada = detalles.stream()
                .map(CompraInventoryMapper::ventaProyectada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gananciaProyectada = ventaProyectada.subtract(compra.getTotal());
        return new CompraResponse(
                compra.getId(),
                compra.getProveedorId(),
                compra.getProveedorDocumento(),
                compra.getProveedorNombre(),
                compra.getTipoComprobante(),
                compra.getSerie(),
                compra.getCorrelativo(),
                compra.getNumeroComprobante(),
                compra.getFechaEmision(),
                compra.getFechaIngreso(),
                compra.getAlmacen().getId(),
                compra.getAlmacen().getCodigo(),
                compra.getAlmacen().getNombre(),
                compra.getTotal(),
                ventaProyectada,
                gananciaProyectada,
                porcentaje(gananciaProyectada, compra.getTotal()),
                compra.getEstado(),
                detalles.stream().map(CompraInventoryMapper::toDetalleResponse).toList()
        );
    }

    public static CompraDetalleResponse toDetalleResponse(CompraDetalle detalle) {
        return new CompraDetalleResponse(
                detalle.getId(),
                detalle.getProducto().getId(),
                detalle.getProducto().getSku(),
                detalle.getProducto().getNombre(),
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                detalle.getPrecioVenta(),
                detalle.getTotal(),
                ventaProyectada(detalle),
                ventaProyectada(detalle).subtract(detalle.getTotal()),
                porcentaje(ventaProyectada(detalle).subtract(detalle.getTotal()), detalle.getTotal()),
                detalle.getCodigoLote(),
                detalle.getFechaFabricacion(),
                detalle.getFechaVencimiento()
        );
    }

    private static BigDecimal ventaProyectada(CompraDetalle detalle) {
        BigDecimal precioVenta = detalle.getPrecioVenta() == null ? BigDecimal.ZERO : detalle.getPrecioVenta();
        return precioVenta.multiply(detalle.getCantidad()).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal porcentaje(BigDecimal valor, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP);
    }
}
