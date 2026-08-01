package com.azurion.saascore.inventory.application.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InventoryOperationalValidatorTest {

    private final InventoryOperationalValidator validator = new InventoryOperationalValidator();

    @Test
    void rejectsServicesAndProductsWithoutStockControl() {
        Producto service = new Producto();
        service.setActivo(true);
        service.setEstado("ACTIVO");
        service.setTipoProducto("SERVICIO");

        assertThatThrownBy(() -> validator.requireStockProduct(service))
                .hasMessageContaining("servicio");

        Producto withoutStock = new Producto();
        withoutStock.setActivo(true);
        withoutStock.setEstado("ACTIVO");
        withoutStock.setTipoProducto("PRODUCTO");
        withoutStock.setManejaStock(false);

        assertThatThrownBy(() -> validator.requireStockProduct(withoutStock))
                .hasMessageContaining("control de stock");
    }

    @Test
    void acceptsOnlyOperationalWarehouse() {
        Almacen active = new Almacen();
        Sucursal activeBranch = new Sucursal();
        activeBranch.setActivo(true);
        active.setActivo(true);
        active.setEstado("ACTIVO");
        active.setSucursal(activeBranch);
        assertThatCode(() -> validator.requireOperationalWarehouse(active)).doesNotThrowAnyException();

        Almacen inactive = new Almacen();
        inactive.setActivo(false);
        inactive.setEstado("INACTIVO");
        assertThatThrownBy(() -> validator.requireOperationalWarehouse(inactive))
                .hasMessageContaining("inactivo");
    }

    @Test
    void rejectsFabricationAfterExpiration() {
        assertThatThrownBy(() -> validator.validateLotDates(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 19)
        )).hasMessageContaining("fabricacion");

        assertThatCode(() -> validator.validateLotDates(
                LocalDate.of(2026, 7, 19),
                LocalDate.of(2026, 7, 20)
        )).doesNotThrowAnyException();
    }
}
