package com.azurion.saascore.inventory.application.services;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class InventoryOperationalValidator {

    public void requireStockProduct(Producto producto) {
        if (!producto.isActivo() || !"ACTIVO".equalsIgnoreCase(producto.getEstado())) {
            throw new BusinessException(
                    "PRODUCTO_INACTIVO",
                    "No se puede operar inventario con un producto inactivo"
            );
        }
        if (!producto.isManejaStock() || "SERVICIO".equalsIgnoreCase(producto.getTipoProducto())) {
            throw new BusinessException(
                    "PRODUCTO_SIN_STOCK",
                    "El producto esta configurado como servicio o sin control de stock"
            );
        }
    }

    public void requireOperationalWarehouse(Almacen almacen) {
        if (!almacen.isActivo() || !"ACTIVO".equalsIgnoreCase(almacen.getEstado())) {
            throw new BusinessException(
                    "ALMACEN_INACTIVO",
                    "No se puede operar inventario en un almacen inactivo"
            );
        }
        if (almacen.getSucursal() == null || !almacen.getSucursal().isActivo()) {
            throw new BusinessException(
                    "SUCURSAL_INACTIVA",
                    "La sucursal del almacen esta inactiva y no permite movimientos"
            );
        }
    }

    public void validateLotDates(LocalDate fechaFabricacion, LocalDate fechaVencimiento) {
        if (fechaFabricacion != null
                && fechaVencimiento != null
                && fechaFabricacion.isAfter(fechaVencimiento)) {
            throw new BusinessException(
                    "LOTE_FECHAS_INVALIDAS",
                    "La fecha de fabricacion no puede ser posterior al vencimiento"
            );
        }
    }
}
