package com.azurion.saascore.inventory.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BuscarProductoPorCodigoUseCaseTest {

    @Test
    void prioritizesExactBarcodeAndReturnsGlobalStock() {
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        StockRepository stockRepository = mock(StockRepository.class);
        Producto producto = producto(7L, "PRD-000007", "7751234567890");

        when(productoRepository.findByCodigoBarrasIgnoreCase("7751234567890"))
                .thenReturn(Optional.of(producto));
        when(stockRepository.sumCantidadByProductoId(7L)).thenReturn(new BigDecimal("23.5"));

        ProductoResponse result = new BuscarProductoPorCodigoUseCase(
                productoRepository,
                stockRepository
        ).execute(" 7751234567890 ");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.stockCantidad()).isEqualByComparingTo("23.5");
        verify(productoRepository).findByCodigoBarrasIgnoreCase("7751234567890");
    }

    @Test
    void returnsNullWhenCodeDoesNotExist() {
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        when(productoRepository.findByCodigoBarrasIgnoreCase("NO-EXISTE"))
                .thenReturn(Optional.empty());
        when(productoRepository.findBySkuIgnoreCase("NO-EXISTE"))
                .thenReturn(Optional.empty());
        when(productoRepository.findByCodigoIgnoreCase("NO-EXISTE"))
                .thenReturn(Optional.empty());

        ProductoResponse result = new BuscarProductoPorCodigoUseCase(
                productoRepository,
                mock(StockRepository.class)
        ).execute("NO-EXISTE");

        assertThat(result).isNull();
    }

    private Producto producto(Long id, String sku, String barcode) {
        Almacen almacen = new Almacen();
        almacen.setId(3L);
        almacen.setCodigo("ALM-01");
        almacen.setNombre("Almacén principal");

        Producto producto = new Producto();
        producto.setId(id);
        producto.setSku(sku);
        producto.setCodigo(sku);
        producto.setCodigoBarras(barcode);
        producto.setNombre("Producto de prueba");
        producto.setPrecio(new BigDecimal("12.50"));
        producto.setPrecioCompraBase(new BigDecimal("8.00"));
        producto.setPrecioVentaBase(new BigDecimal("12.50"));
        producto.setCostoPromedio(new BigDecimal("8.00"));
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setStockMinimoGlobal(BigDecimal.ZERO);
        producto.setAlmacen(almacen);
        producto.setActivo(true);
        producto.setEstado("ACTIVO");
        return producto;
    }
}
