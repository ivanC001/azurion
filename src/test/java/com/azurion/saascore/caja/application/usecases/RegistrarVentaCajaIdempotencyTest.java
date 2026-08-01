package com.azurion.saascore.caja.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaRequest;
import com.azurion.saascore.caja.application.dto.RegistrarVentaCajaResponse;
import com.azurion.saascore.caja.application.dto.TipoComprobanteVenta;
import com.azurion.saascore.caja.application.services.CajaTurnoService;
import com.azurion.saascore.caja.application.services.VentaSucursalStockPolicy;
import com.azurion.saascore.caja.domain.entities.CajaTurno;
import com.azurion.saascore.caja.domain.repositories.CajaTurnoRepository;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.facturacion.application.services.FacturadorEmissionCapabilityPolicy;
import com.azurion.saascore.inventory.application.usecases.StockMovimientoUseCase;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.tributacion.application.services.TaxResolverService;
import com.azurion.saascore.ventas.application.dto.VentaResponse;
import com.azurion.saascore.ventas.application.usecases.RegisterVentaUseCase;
import com.azurion.shared.util.RequestFingerprint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegistrarVentaCajaIdempotencyTest {

    @Test
    void retryReturnsCompletedSaleWithoutMovingStockOrDispatchingAgain() {
        CajaTurnoService turnoService = mock(CajaTurnoService.class);
        StockMovimientoUseCase stockMovimientoUseCase = mock(StockMovimientoUseCase.class);
        RegisterVentaUseCase registerVentaUseCase = mock(RegisterVentaUseCase.class);
        DispatchVentaFacturacionAsyncUseCase dispatcher = mock(DispatchVentaFacturacionAsyncUseCase.class);
        CajaTurno turno = new CajaTurno();
        turno.setId(7L);
        when(turnoService.findForUpdate(7L)).thenReturn(turno);

        RegistrarVentaCajaRequest request = new RegistrarVentaCajaRequest(
                TipoComprobanteVenta.TICKET_VENTA,
                BigDecimal.TEN,
                null,
                null,
                null,
                null,
                null,
                "PEN",
                BigDecimal.ONE,
                "CONTADO",
                "EFECTIVO",
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                "Venta de prueba",
                List.of(),
                "sale-operation-123"
        );
        VentaResponse completedSale = new VentaResponse(
                90L,
                "VENTA-OP-ABC",
                "-",
                "CLIENTES VARIOS",
                "PEN",
                BigDecimal.TEN,
                7L,
                "CONTADO",
                "EFECTIVO",
                OffsetDateTime.now(),
                "PENDIENTE",
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now()
        );
        String requestHash = RequestFingerprint.sha256(7L, request);
        when(registerVentaUseCase.findCompletedOperation("sale-operation-123"))
                .thenReturn(Optional.of(
                        new RegisterVentaUseCase.CompletedVentaOperation(completedSale, requestHash)
                ));

        RegistrarVentaCajaUseCase useCase = new RegistrarVentaCajaUseCase(
                mock(CajaTurnoRepository.class),
                mock(CajaMovimientoService.class),
                turnoService,
                mock(EmpresaRepository.class),
                mock(ClienteRepository.class),
                mock(ProductoRepository.class),
                stockMovimientoUseCase,
                registerVentaUseCase,
                dispatcher,
                mock(VentaSucursalStockPolicy.class),
                mock(TaxResolverService.class),
                mock(FacturadorEmissionCapabilityPolicy.class)
        );

        RegistrarVentaCajaResponse response = useCase.execute(7L, request);

        assertThat(response.venta().id()).isEqualTo(90L);
        assertThat(response.facturacion().message()).contains("sin duplicarla");
        verify(turnoService, never()).requireOpen(turno);
        verifyNoInteractions(stockMovimientoUseCase, dispatcher);
    }
}
