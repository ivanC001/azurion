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
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            Duration.ofHours(1)
    );

    private final VentaFacturacionOutboxRepository outboxRepository;
    private final ProcessVentaFacturacionAsyncUseCase processUseCase;
    private final ObjectMapper objectMapper;
    @Qualifier("eventExecutor")
    private final Executor eventExecutor;

    @Scheduled(
            initialDelayString = "${azurion.facturador.outbox.initial-delay-millis:5000}",
            fixedDelayString = "${azurion.facturador.outbox.poll-delay-millis:2000}"
    )
    public void poll() {
        LocalDateTime now = LocalDateTime.now();
        outboxRepository.recoverStuck(now.minusMinutes(10), now);
        for (VentaFacturacionOutbox candidate : outboxRepository
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(List.of("PENDING", "RETRY"), now)) {
            if (outboxRepository.claim(candidate.getId(), now) != 1) {
                continue;
            }
            outboxRepository.findById(candidate.getId())
                    .ifPresent(job -> eventExecutor.execute(() -> process(job)));
        }
    }

    @Scheduled(cron = "${azurion.facturador.outbox.cleanup-cron:0 30 3 * * *}")
    public void cleanupCompleted() {
        int deleted = outboxRepository.deleteCompletedBefore(LocalDateTime.now().minusDays(30));
        if (deleted > 0) {
            log.info("Se eliminaron {} tareas de facturacion completadas con mas de 30 dias", deleted);
        }
    }

    private void process(VentaFacturacionOutbox job) {
        try {
            processUseCase.execute(toTask(job));
            outboxRepository.markCompleted(job.getId(), LocalDateTime.now());
        } catch (Exception error) {
            int attempts = job.getAttempts() == null ? 1 : job.getAttempts();
            boolean exhausted = attempts >= MAX_ATTEMPTS;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextAttempt = exhausted
                    ? now
                    : now.plus(RETRY_DELAYS.get(Math.min(attempts - 1, RETRY_DELAYS.size() - 1)));
            outboxRepository.markFailedAttempt(
                    job.getId(),
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
