package com.azurion.saascore.caja.application.usecases;

import com.azurion.saascore.caja.application.dto.VentaFacturacionAsyncTask;
import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import com.azurion.saascore.caja.domain.repositories.VentaFacturacionOutboxRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispatchVentaFacturacionAsyncUseCase {

    private final VentaFacturacionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

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
    }

    private String writePayload(VentaFacturacionAsyncTask task) {
        try {
            return objectMapper.writeValueAsString(task.payload());
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FACTURACION_PAYLOAD_INVALIDO", "No se pudo guardar la tarea de facturacion");
        }
    }
}
