package com.azurion.saascore.caja.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import com.azurion.saascore.caja.domain.repositories.VentaFacturacionOutboxRepository;
import com.azurion.saascore.ventas.application.dto.VentaStatusRealtimeEvent;
import com.azurion.saascore.ventas.domain.entities.Venta;
import com.azurion.saascore.ventas.domain.repositories.VentaRepository;
import com.azurion.saascore.ventas.infrastructure.realtime.VentaStatusRealtimeStreamService;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class RetryVentaFacturacionUseCase {

    private final VentaRepository ventaRepository;
    private final VentaFacturacionOutboxRepository outboxRepository;
    private final VentaStatusRealtimeStreamService realtimeStreamService;

    @Transactional
    public void execute(Long ventaId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || "public".equalsIgnoreCase(tenantId)) {
            throw BusinessException.forbidden("TENANT_REQUIRED", "No existe un tenant activo");
        }

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> BusinessException.notFound("VENTA_NOT_FOUND", "La venta no existe"));
        String estado = venta.getFacturacionEstado() == null
                ? ""
                : venta.getFacturacionEstado().trim().toUpperCase();

        if (Venta.FACTURACION_ESTADO_ACEPTADO.equals(estado)) {
            throw BusinessException.conflict(
                    "DOCUMENTO_YA_GENERADO",
                    "El documento oficial ya fue generado"
            );
        }
        if (Venta.FACTURACION_ESTADO_PENDIENTE.equals(estado)
                || Venta.FACTURACION_ESTADO_PROCESANDO.equals(estado)) {
            throw BusinessException.conflict(
                    "DOCUMENTO_EN_PROCESO",
                    "El documento ya se encuentra en proceso"
            );
        }
        if (Venta.FACTURACION_ESTADO_RECHAZADO.equals(estado)) {
            throw BusinessException.conflict(
                    "DOCUMENTO_RECHAZADO",
                    "El documento fue rechazado y requiere corregir sus datos antes de emitir uno nuevo"
            );
        }

        VentaFacturacionOutbox job = outboxRepository
                .findFirstByTenantIdAndVentaIdOrderByIdDesc(tenantId, ventaId)
                .orElseThrow(() -> BusinessException.notFound(
                        "FACTURACION_TASK_NOT_FOUND",
                        "No existe una tarea de emision para esta venta"
                ));

        if ("PROCESSING".equalsIgnoreCase(job.getStatus())
                || "PENDING".equalsIgnoreCase(job.getStatus())
                || "RETRY".equalsIgnoreCase(job.getStatus())) {
            throw BusinessException.conflict(
                    "DOCUMENTO_EN_PROCESO",
                    "El documento ya se encuentra en proceso"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        job.setStatus("PENDING");
        job.setAttempts(0);
        job.setNextAttemptAt(now);
        job.setLastError(null);
        job.setLeaseOwner(null);
        job.setLeaseUntil(null);
        job.setHeartbeatAt(null);
        outboxRepository.save(job);

        venta.setFacturacionEstado(Venta.FACTURACION_ESTADO_PENDIENTE);
        venta.setFacturadorMensaje("Reintento de generacion solicitado");
        venta.setFacturacionActualizadoEn(OffsetDateTime.now());
        ventaRepository.save(venta);
        publishAfterCommit(VentaStatusRealtimeEvent.fromVenta(tenantId, "MANUAL_RETRY", venta));
    }

    private void publishAfterCommit(VentaStatusRealtimeEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            realtimeStreamService.publish(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimeStreamService.publish(event);
            }
        });
    }
}
