package com.azurion.saascore.inventory.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.inventory.application.dto.CreateProductoRapidoRequest;
import com.azurion.saascore.inventory.application.dto.CreateProductoRequest;
import com.azurion.saascore.inventory.application.dto.ProductoResponse;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.application.services.ProductoSkuGenerator;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateProductoRapidoUseCaseTest {

    @Test
    void generatesSkuAndRegistersOpeningStockInSameFlow() {
        CreateProductoUseCase createUseCase = mock(CreateProductoUseCase.class);
        StockMovimientoUseCase stockUseCase = mock(StockMovimientoUseCase.class);
        ProductoSkuGenerator skuGenerator = mock(ProductoSkuGenerator.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        StockRepository stockRepository = mock(StockRepository.class);

        ProductoResponse created = mock(ProductoResponse.class);
        when(created.id()).thenReturn(21L);
        when(skuGenerator.nextSku()).thenReturn("PRD-000021");
        when(productoRepository.findBySkuIgnoreCase("PRD-000021")).thenReturn(Optional.empty());
        when(createUseCase.execute(any(CreateProductoRequest.class))).thenReturn(created);

        Producto persisted = producto(21L);
        when(productoRepository.findById(21L)).thenReturn(Optional.of(persisted));
        when(stockRepository.sumCantidadByProductoId(21L)).thenReturn(new BigDecimal("8"));

        CreateProductoRapidoUseCase useCase = new CreateProductoRapidoUseCase(
                createUseCase,
                stockUseCase,
                skuGenerator,
                productoRepository,
                stockRepository
        );

        ProductoResponse result = useCase.execute(new CreateProductoRapidoRequest(
                "7750000000021",
                null,
                "Producto rápido",
                null,
                2L,
                null,
                "PRODUCTO",
                new BigDecimal("15.90"),
                new BigDecimal("10.00"),
                new BigDecimal("8"),
                4L,
                false,
                new BigDecimal("2"),
                null,
                null,
                null,
                null
        ));

        ArgumentCaptor<CreateProductoRequest> createCaptor =
                ArgumentCaptor.forClass(CreateProductoRequest.class);
        verify(createUseCase).execute(createCaptor.capture());
        assertThat(createCaptor.getValue().sku()).isEqualTo("PRD-000021");
        assertThat(createCaptor.getValue().codigo()).isEqualTo("PRD-000021");

        ArgumentCaptor<StockMovimientoRequest> stockCaptor =
                ArgumentCaptor.forClass(StockMovimientoRequest.class);
        verify(stockUseCase).execute(stockCaptor.capture());
        assertThat(stockCaptor.getValue().motivo()).isEqualTo("STOCK_INICIAL");
        assertThat(stockCaptor.getValue().cantidad()).isEqualByComparingTo("8");
        assertThat(result.stockCantidad()).isEqualByComparingTo("8");
    }

    private Producto producto(Long id) {
        Almacen almacen = new Almacen();
        almacen.setId(4L);
        almacen.setCodigo("ALM-01");
        almacen.setNombre("Almacén principal");

        Producto producto = new Producto();
        producto.setId(id);
        producto.setSku("PRD-000021");
        producto.setCodigo("PRD-000021");
        producto.setCodigoBarras("7750000000021");
        producto.setNombre("Producto rápido");
        producto.setPrecio(new BigDecimal("15.90"));
        producto.setPrecioCompraBase(new BigDecimal("10.00"));
        producto.setPrecioVentaBase(new BigDecimal("15.90"));
        producto.setCostoPromedio(new BigDecimal("10.00"));
        producto.setStockMinimo(new BigDecimal("2"));
        producto.setStockMinimoGlobal(new BigDecimal("2"));
        producto.setAlmacen(almacen);
        producto.setActivo(true);
        producto.setEstado("ACTIVO");
        return producto;
    }
}
