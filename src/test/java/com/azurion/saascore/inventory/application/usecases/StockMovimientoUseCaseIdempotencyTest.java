package com.azurion.saascore.inventory.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.inventory.application.dto.KardexMovimientoResponse;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.application.services.InventoryOperationalValidator;
import com.azurion.saascore.inventory.domain.entities.InventoryOperationRequest;
import com.azurion.saascore.inventory.domain.entities.KardexMovimiento;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.entities.Stock;
import com.azurion.saascore.inventory.domain.repositories.InventoryOperationRequestRepository;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import com.azurion.saascore.inventory.domain.repositories.LoteRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockLoteRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.persistence.BusinessOperationLockService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockMovimientoUseCaseIdempotencyTest {

    private ProductoRepository productoRepository;
    private AlmacenRepository almacenRepository;
    private StockRepository stockRepository;
    private KardexMovimientoRepository kardexRepository;
    private InventoryOperationRequestRepository operationRepository;
    private StockMovimientoUseCase useCase;
    private InventoryOperationRequest completedOperation;

    @BeforeEach
    void setUp() {
        productoRepository = mock(ProductoRepository.class);
        almacenRepository = mock(AlmacenRepository.class);
        stockRepository = mock(StockRepository.class);
        kardexRepository = mock(KardexMovimientoRepository.class);
        operationRepository = mock(InventoryOperationRequestRepository.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);

        Producto producto = new Producto();
        producto.setId(15L);
        producto.setSku("SKU-15");
        producto.setNombre("Producto de prueba");
        producto.setPrecioVentaBase(BigDecimal.TEN);
        producto.setActivo(true);
        producto.setManejaStock(true);

        Almacen almacen = new Almacen();
        almacen.setId(2L);
        almacen.setCodigo("ALM-02");
        almacen.setActivo(true);

        Stock stock = new Stock();
        stock.setId(20L);
        stock.setProducto(producto);
        stock.setAlmacen(almacen);
        stock.setCantidad(BigDecimal.ZERO);

        AtomicReference<KardexMovimiento> savedMovement = new AtomicReference<>();
        when(authorizationService.currentUsuarioId()).thenReturn(9L);
        when(productoRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(producto));
        when(almacenRepository.findById(2L)).thenReturn(Optional.of(almacen));
        when(stockRepository.findByProductoIdAndAlmacenId(15L, 2L)).thenReturn(Optional.of(stock));
        when(stockRepository.sumCantidadByProductoId(15L)).thenReturn(BigDecimal.ZERO);
        when(operationRepository.findByOperationKey("operation-123")).thenReturn(Optional.empty());
        when(kardexRepository.save(any(KardexMovimiento.class))).thenAnswer(invocation -> {
            KardexMovimiento movimiento = invocation.getArgument(0);
            movimiento.setId(100L);
            savedMovement.set(movimiento);
            return movimiento;
        });
        when(kardexRepository.getReferenceById(100L)).thenAnswer(invocation -> savedMovement.get());
        when(operationRepository.saveAndFlush(any(InventoryOperationRequest.class))).thenAnswer(invocation -> {
            completedOperation = invocation.getArgument(0);
            completedOperation.setId(200L);
            return completedOperation;
        });

        useCase = new StockMovimientoUseCase(
                productoRepository,
                almacenRepository,
                stockRepository,
                mock(LoteRepository.class),
                mock(StockLoteRepository.class),
                kardexRepository,
                operationRepository,
                mock(BusinessOperationLockService.class),
                authorizationService,
                mock(InventoryOperationalValidator.class)
        );
    }

    @Test
    void repeatedOperationReturnsOriginalMovementWithoutChangingStockTwice() {
        StockMovimientoRequest request = request(BigDecimal.valueOf(5));

        KardexMovimientoResponse first = useCase.execute(request);
        when(operationRepository.findByOperationKey("operation-123"))
                .thenReturn(Optional.of(completedOperation));
        KardexMovimientoResponse repeated = useCase.execute(request);

        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(repeated.saldoResultante()).isEqualByComparingTo("5");
        verify(stockRepository, times(1)).save(any(Stock.class));
        verify(kardexRepository, times(1)).save(any(KardexMovimiento.class));
        verify(operationRepository, times(1)).saveAndFlush(any(InventoryOperationRequest.class));
    }

    @Test
    void repeatedKeyWithDifferentPayloadIsRejected() {
        useCase.execute(request(BigDecimal.valueOf(5)));
        when(operationRepository.findByOperationKey("operation-123"))
                .thenReturn(Optional.of(completedOperation));

        assertThatThrownBy(() -> useCase.execute(request(BigDecimal.valueOf(6))))
                .hasMessageContaining("datos diferentes");
        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    private StockMovimientoRequest request(BigDecimal cantidad) {
        return new StockMovimientoRequest(
                15L,
                2L,
                null,
                null,
                null,
                null,
                null,
                "ENTRADA",
                "COMPRA",
                cantidad,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.TEN,
                "9",
                "COMPRA:1",
                "operation-123"
        );
    }
}
