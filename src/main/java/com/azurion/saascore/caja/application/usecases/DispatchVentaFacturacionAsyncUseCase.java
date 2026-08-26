package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.VentaFacturacionAsyncTask;
import com.azurion.saascore.caja.application.events.VentaFacturacionQueuedEvent;
import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import com.azurion.saascore.caja.domain.repositories.VentaFacturacionOutboxRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class DispatchVentaFacturacionAsyncUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DispatchVentaFacturacionAsyncUseCase.class);
    private final VentaFacturacionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public void dispatch(VentaFacturacionAsyncTask task) {
        VentaFacturacionOutbox job = new VentaFacturacionOutbox();
        job.setTenantId(task.tenantId());
        job.setTenantRuc(task.tenantRuc());
        job.setVentaId(task.ventaId());
        job.setExternalId(task.externalId());
        job.setEndpoint(task.endpoint());
        job.setTipoComprobante(task.tipoComprobante());
        job.setPayloadJson(writePayload(task));
        job.setStatus("PENDING");
        job.setAttempts(0);
        job.setNextAttemptAt(LocalDateTime.now());
        outboxRepository.save(job);
        publishAfterCommit(job.getId());
    }

    private void publishAfterCommit(Long outboxId) {
        Runnable publish = () -> {
            try {
                eventPublisher.publishEvent(new VentaFacturacionQueuedEvent(outboxId));
            } catch (RuntimeException exception) {
                // El sondeo periodico conserva la entrega si el ejecutor inmediato esta saturado.
                log.warn("No se pudo despertar inmediatamente la tarea de facturacion {}", outboxId, exception);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

    private String writePayload(VentaFacturacionAsyncTask task) {
        try {
            return objectMapper.writeValueAsString(task.payload());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FACTURACION_PAYLOAD_INVALIDO", "No se pudo guardar la tarea de facturacion");
        }
    }
}
