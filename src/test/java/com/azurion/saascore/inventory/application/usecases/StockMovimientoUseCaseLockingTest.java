package com.azurion.saascore.inventory.application.usecases;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.inventory.application.dto.StockMovimientoRequest;
import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.KardexMovimientoRepository;
import com.azurion.saascore.inventory.domain.repositories.LoteRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.inventory.domain.repositories.StockLoteRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StockMovimientoUseCaseLockingTest {

    @Test
    void locksProductBeforeReadingOrChangingStock() {
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        Producto inactive = new Producto();
        inactive.setActivo(false);
        inactive.setEstado("INACTIVO");
        when(authorizationService.currentUsuarioId()).thenReturn(9L);
        when(productoRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(inactive));

        StockMovimientoUseCase useCase = new StockMovimientoUseCase(
                productoRepository,
                mock(AlmacenRepository.class),
                mock(StockRepository.class),
                mock(LoteRepository.class),
                mock(StockLoteRepository.class),
                mock(KardexMovimientoRepository.class),
                authorizationService
        );

        StockMovimientoRequest request = new StockMovimientoRequest(
                15L, 2L, null, null, null, null, null,
                "SALIDA", "VENTA", BigDecimal.ONE,
                null, null, null, "9", "VENTA:1"
        );

        assertThatThrownBy(() -> useCase.execute(request))
                .hasMessageContaining("producto inactivo");
        verify(productoRepository).findByIdForUpdate(15L);
    }
}
