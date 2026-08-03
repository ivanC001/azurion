package com.azurion.saascore.caja.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import com.azurion.saascore.caja.domain.repositories.VentaFacturacionOutboxRepository;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.saascore.ventas.infrastructure.realtime.VentaStatusRealtimeStreamService;
import com.azurion.shared.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RetryVentaFacturacionUseCaseTest {

    private final VentaRepository ventaRepository = mock(VentaRepository.class);
    private final VentaFacturacionOutboxRepository outboxRepository =
            mock(VentaFacturacionOutboxRepository.class);
    private final VentaStatusRealtimeStreamService realtimeStreamService =
            mock(VentaStatusRealtimeStreamService.class);
    private final RetryVentaFacturacionUseCase useCase = new RetryVentaFacturacionUseCase(
            ventaRepository,
            outboxRepository,
            realtimeStreamService
    );

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void failedDocumentIsRequeuedWithoutCreatingAnotherTask() {
        TenantContext.setTenantId("tenant_demo");
        Venta venta = venta(15L, Venta.FACTURACION_ESTADO_ERROR);
        VentaFacturacionOutbox job = new VentaFacturacionOutbox();
        job.setStatus("FAILED");
        job.setAttempts(5);
        job.setLastError("Facturador temporalmente no disponible");

        when(ventaRepository.findById(15L)).thenReturn(Optional.of(venta));
        when(outboxRepository.findFirstByTenantIdAndVentaIdOrderByIdDesc("tenant_demo", 15L))
                .thenReturn(Optional.of(job));

        useCase.execute(15L);

        assertThat(job.getStatus()).isEqualTo("PENDING");
        assertThat(job.getAttempts()).isZero();
        assertThat(job.getLastError()).isNull();
        assertThat(venta.getFacturacionEstado()).isEqualTo(Venta.FACTURACION_ESTADO_PENDIENTE);
        verify(outboxRepository).save(job);
        verify(ventaRepository).save(venta);
        verify(realtimeStreamService).publish(
                org.mockito.ArgumentMatchers.argThat(event ->
                        "MANUAL_RETRY".equals(event.source())
                                && "VENTA-15".equals(event.externalId()))
        );
    }

    @Test
    void acceptedDocumentCannotBeRequeued() {
        TenantContext.setTenantId("tenant_demo");
        when(ventaRepository.findById(20L))
                .thenReturn(Optional.of(venta(20L, Venta.FACTURACION_ESTADO_ACEPTADO)));

        assertThatThrownBy(() -> useCase.execute(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue generado");

        verifyNoInteractions(outboxRepository, realtimeStreamService);
    }

    private Venta venta(Long id, String estado) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setExternalId("VENTA-" + id);
        venta.setFacturacionEstado(estado);
        return venta;
    }
}
