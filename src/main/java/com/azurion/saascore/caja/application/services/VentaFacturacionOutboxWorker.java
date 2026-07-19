package com.azurion.saascore.caja.application.services;

import com.azurion.saascore.caja.application.dto.VentaFacturacionAsyncTask;
import com.azurion.saascore.caja.application.usecases.ProcessVentaFacturacionAsyncUseCase;
import com.azurion.saascore.caja.domain.entities.VentaFacturacionOutbox;
import com.azurion.saascore.caja.domain.repositories.VentaFacturacionOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@Service
@ConditionalOnProperty(name = "azurion.facturador.outbox.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class VentaFacturacionOutboxWorker {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(2);
    private static final long HEARTBEAT_SECONDS = 30;
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)
    );

    private final VentaFacturacionOutboxRepository outboxRepository;
    private final ProcessVentaFacturacionAsyncUseCase processUseCase;
    private final ObjectMapper objectMapper;
    @Qualifier("facturacionExecutor")
    private final Executor facturacionExecutor;
    @Qualifier("facturacionHeartbeatExecutor")
    private final ScheduledExecutorService heartbeatExecutor;
    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(
            initialDelayString = "${azurion.facturador.outbox.initial-delay-millis:5000}",
            fixedDelayString = "${azurion.facturador.outbox.poll-delay-millis:2000}"
    )
    public void poll() {
        LocalDateTime now = LocalDateTime.now();
        outboxRepository.recoverExpiredLeases(now);
        for (VentaFacturacionOutbox candidate : outboxRepository
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(List.of("PENDING", "RETRY"), now)) {
            if (outboxRepository.claim(candidate.getId(), workerId, now, now.plus(LEASE_DURATION)) != 1) {
                continue;
            }
            outboxRepository.findByIdAndStatusAndLeaseOwner(candidate.getId(), "PROCESSING", workerId)
                    .ifPresent(this::submit);
        }
    }

    @Scheduled(cron = "${azurion.facturador.outbox.cleanup-cron:0 30 3 * * *}")
    public void cleanupCompleted() {
        int deleted = outboxRepository.deleteCompletedBefore(LocalDateTime.now().minusDays(30));
        if (deleted > 0) {
            log.info("Se eliminaron {} tareas de facturacion completadas con mas de 30 dias", deleted);
        }
    }

    private void submit(VentaFacturacionOutbox job) {
        try {
            facturacionExecutor.execute(() -> process(job));
        } catch (RejectedExecutionException error) {
            LocalDateTime now = LocalDateTime.now();
            outboxRepository.markFailedAttempt(
                    job.getId(), workerId, "RETRY", now.plusSeconds(30),
                    "Pool de facturacion temporalmente saturado", now
            );
            log.warn("Pool de facturacion saturado; tarea {} reprogramada", job.getExternalId());
        }
    }

    private void process(VentaFacturacionOutbox job) {
        ScheduledFuture<?> heartbeat = heartbeatExecutor.scheduleAtFixedRate(
                () -> renewLease(job),
                HEARTBEAT_SECONDS,
                HEARTBEAT_SECONDS,
                TimeUnit.SECONDS
        );
        try {
            processUseCase.execute(toTask(job));
            int updated = outboxRepository.markCompleted(job.getId(), workerId, LocalDateTime.now());
            if (updated != 1) {
                log.error("La tarea {} termino sin conservar la propiedad del lease", job.getExternalId());
            }
        } catch (Exception error) {
            int attempts = job.getAttempts() == null ? 1 : job.getAttempts();
            boolean exhausted = attempts >= MAX_ATTEMPTS;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextAttempt = exhausted
                    ? now
                    : now.plus(RETRY_DELAYS.get(Math.min(attempts - 1, RETRY_DELAYS.size() - 1)));
            outboxRepository.markFailedAttempt(
                    job.getId(),
                    workerId,
                    exhausted ? "FAILED" : "RETRY",
                    nextAttempt,
                    trimError(error),
                    now
            );
            log.warn(
                    "Facturacion {} fallo en intento {}. Estado de cola: {}",
                    job.getExternalId(),
                    attempts,
                    exhausted ? "FAILED" : "RETRY"
            );
        } finally {
            heartbeat.cancel(false);
        }
    }

    private void renewLease(VentaFacturacionOutbox job) {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updated = outboxRepository.heartbeat(job.getId(), workerId, now, now.plus(LEASE_DURATION));
            if (updated != 1) {
                log.error("No se pudo renovar el lease de la tarea {}", job.getExternalId());
            }
        } catch (RuntimeException error) {
            log.error("Fallo el heartbeat de la tarea {}", job.getExternalId(), error);
        }
    }

    private VentaFacturacionAsyncTask toTask(VentaFacturacionOutbox job) {
        try {
            Map<String, Object> payload = objectMapper.readValue(job.getPayloadJson(), new TypeReference<>() { });
            return new VentaFacturacionAsyncTask(
                    job.getTenantId(),
                    job.getTenantRuc(),
                    job.getVentaId(),
                    job.getExternalId(),
                    job.getEndpoint(),
                    job.getTipoComprobante(),
                    payload
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("El payload persistido de facturacion no es valido", ex);
        }
    }

    private String trimError(Exception error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
