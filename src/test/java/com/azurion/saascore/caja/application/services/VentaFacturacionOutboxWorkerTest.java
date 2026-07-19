package com.azurion.saascore.caja.application.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.caja.application.usecases.ProcessVentaFacturacionAsyncUseCase;
import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import com.azurion.saascore.caja.domain.repositories.VentaFacturacionOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VentaFacturacionOutboxWorkerTest {

    private final VentaFacturacionOutboxRepository repository = mock(VentaFacturacionOutboxRepository.class);
    private final ProcessVentaFacturacionAsyncUseCase processUseCase = mock(ProcessVentaFacturacionAsyncUseCase.class);
    private final VentaFacturacionOutboxWorker worker = new VentaFacturacionOutboxWorker(
            repository,
            processUseCase,
            new ObjectMapper(),
            Runnable::run
    );

    @Test
    void claimsAndCompletesAJob() {
        VentaFacturacionOutbox job = job(1);
        when(repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(repository.claim(eq(job.getId()), any(LocalDateTime.class))).thenReturn(1);
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));

        worker.poll();

        verify(processUseCase).execute(any());
        verify(repository).markCompleted(eq(job.getId()), any(LocalDateTime.class));
        verify(repository, never()).markFailedAttempt(eq(job.getId()), any(), any(), any(), any());
    }

    @Test
    void schedulesARetryAfterAProcessingFailure() {
        VentaFacturacionOutbox job = job(1);
        when(repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(repository.claim(eq(job.getId()), any(LocalDateTime.class))).thenReturn(1);
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));
        doThrow(new IllegalStateException("facturador temporalmente no disponible"))
                .when(processUseCase).execute(any());

        worker.poll();

        verify(repository).markFailedAttempt(
                eq(job.getId()),
                eq("RETRY"),
                any(LocalDateTime.class),
                eq("facturador temporalmente no disponible"),
                any(LocalDateTime.class)
        );
        verify(repository, never()).markCompleted(eq(job.getId()), any());
    }

    @Test
    void marksTheJobFailedAfterTheMaximumAttempt() {
        VentaFacturacionOutbox job = job(5);
        when(repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(any(), any()))
                .thenReturn(List.of(job));
        when(repository.claim(eq(job.getId()), any(LocalDateTime.class))).thenReturn(1);
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));
        doThrow(new IllegalStateException("error definitivo")).when(processUseCase).execute(any());

        worker.poll();

        verify(repository).markFailedAttempt(
                eq(job.getId()),
                eq("FAILED"),
                any(LocalDateTime.class),
                eq("error definitivo"),
                any(LocalDateTime.class)
        );
    }

    private VentaFacturacionOutbox job(int attempts) {
        VentaFacturacionOutbox job = new VentaFacturacionOutbox();
        job.setId(77L);
        job.setTenantId("tenant-1");
        job.setTenantRuc("20123456789");
        job.setVentaId(15L);
        job.setExternalId("sale-15");
        job.setEndpoint("/documents");
        job.setTipoComprobante("FACTURA");
        job.setPayloadJson("{}");
        job.setAttempts(attempts);
        return job;
    }
}
