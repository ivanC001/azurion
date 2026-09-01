package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.SendWhatsappTemplateRequest;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappConversation;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappReengagementOutbox;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappConversationRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Envia los reenganches programados cuando llega su fecha.
 *
 * <p>Sondea la cola del esquema {@code public} sin contexto de tenant y lo fija recien
 * al procesar cada tarea, igual que {@code VentaFacturacionOutboxWorker}.
 *
 * <p>Las condiciones se vuelven a evaluar al enviar, no al programar: entre que se
 * programa un reenganche y llega su fecha pasa una semana, y en ese lapso el cliente
 * pudo responder, darse de baja o desaparecer.
 */
@Service
@ConditionalOnProperty(
        name = "azurion.crm.reengagement.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class WhatsappReengagementWorker {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WhatsappReengagementWorker.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);
    private static final long CUSTOMER_SERVICE_WINDOW_HOURS = 24;
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(5),
            Duration.ofMinutes(30),
            Duration.ofHours(2),
            Duration.ofHours(6)
    );

    /**
     * Rechazos definitivos de Meta: reintentar no cambia el resultado y cada intento
     * gasta reputacion del numero.
     */
    private static final Set<String> NON_RETRYABLE = Set.of(
            "CRM_WHATSAPP_PLANTILLA_NO_ENCONTRADA",
            "CRM_WHATSAPP_PLANTILLA_RECHAZADA",
            "CRM_WHATSAPP_DESTINATARIO_NO_DISPONIBLE",
            "CRM_WHATSAPP_CONFIG_INCOMPLETA",
            "CRM_WHATSAPP_PARAMETROS_INVALIDOS",
            "CRM_PROSPECTO_NO_ENCONTRADO"
    );

    private final CrmWhatsappReengagementOutboxRepository outboxRepository;
    private final CrmProspectoRepository prospectoRepository;
    private final CrmWhatsappConversationRepository conversationRepository;
    private final WhatsappIntegrationService whatsappIntegrationService;
    private final WhatsappReengagementService reengagementService;
    private final WhatsappAutoReplyConfigurationService autoReplyConfigurationService;

    @Value("${azurion.crm.reengagement.business-hour-start:9}")
    private int businessHourStart;

    @Value("${azurion.crm.reengagement.business-hour-end:20}")
    private int businessHourEnd;

    @Value("${azurion.crm.reengagement.business-days:1,2,3,4,5,6}")
    private String businessDays;

    private final String workerId = UUID.randomUUID().toString();

    @Scheduled(
            initialDelayString = "${azurion.crm.reengagement.initial-delay-millis:20000}",
            fixedDelayString = "${azurion.crm.reengagement.poll-delay-millis:60000}"
    )
    public void poll() {
        LocalDateTime now = LocalDateTime.now();
        try {
            outboxRepository.recoverExpiredLeases(now);
        } catch (RuntimeException error) {
            log.error("No se pudieron recuperar los leases vencidos de reenganche", error);
            return;
        }

        List<CrmWhatsappReengagementOutbox> candidates = outboxRepository
                .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(List.of("PENDING", "RETRY"), now);
        for (CrmWhatsappReengagementOutbox candidate : candidates.stream().limit(BATCH_SIZE).toList()) {
            try {
                claimAndProcess(candidate.getId());
            } catch (RuntimeException error) {
                // Una tarea rota no puede detener al resto del lote.
                log.error("Fallo inesperado procesando el reenganche {}", candidate.getId(), error);
            }
        }
    }

    @Scheduled(cron = "${azurion.crm.reengagement.cleanup-cron:0 45 3 * * *}")
    public void cleanupResolved() {
        int deleted = outboxRepository.deleteResolvedBefore(LocalDateTime.now().minusDays(90));
        if (deleted > 0) {
            log.info("Se eliminaron {} reenganches resueltos con mas de 90 dias", deleted);
        }
    }

    private void claimAndProcess(Long jobId) {
        LocalDateTime now = LocalDateTime.now();
        if (outboxRepository.claim(jobId, workerId, now, now.plus(LEASE_DURATION)) != 1) {
            return;
        }
        outboxRepository.findByIdAndStatusAndLeaseOwner(jobId, "PROCESSING", workerId)
                .ifPresent(this::process);
    }

    private void process(CrmWhatsappReengagementOutbox job) {
        String previousTenant = TenantContext.getTenantId();
        TenantContext.setTenantId(job.getTenantId());
        try {
            String skipReason = skipReason(job);
            if (skipReason != null) {
                resolve(job, "SKIPPED", skipReason);
                return;
            }

            ZonedDateTime tenantNow = ZonedDateTime.now(tenantZone());
            if (!insideBusinessHours(tenantNow)) {
                // No es un fallo: se corre a la proxima franja habil sin gastar intentos.
                reschedule(job, nextOpening(tenantNow));
                return;
            }

            whatsappIntegrationService.sendTemplate(
                    job.getProspectoId(),
                    new SendWhatsappTemplateRequest(
                            job.getPlantillaNombre(),
                            job.getPlantillaIdioma(),
                            reengagementService.readParameters(job.getParametrosJson())
                    )
            );
            resolve(job, "SENT", "Plantilla de reenganche enviada");
            log.info(
                    "Reenganche {} enviado tenant={} prospecto={} plantilla={}",
                    job.getId(),
                    job.getTenantId(),
                    job.getProspectoId(),
                    job.getPlantillaNombre()
            );
        } catch (RuntimeException error) {
            handleFailure(job, error);
        } finally {
            restoreTenant(previousTenant);
        }
    }

    /**
     * Motivo por el que ya no corresponde enviar, o {@code null} si hay que seguir.
     */
    private String skipReason(CrmWhatsappReengagementOutbox job) {
        CrmProspecto prospecto = prospectoRepository.findById(job.getProspectoId()).orElse(null);
        if (prospecto == null) {
            return "El prospecto ya no existe";
        }
        if (prospecto.getWhatsappOptoutEn() != null) {
            return "El prospecto pidio no recibir mas mensajes";
        }

        CrmWhatsappConversation conversation =
                conversationRepository.findByProspecto_Id(job.getProspectoId()).orElse(null);
        OffsetDateTime lastInbound = conversation == null ? null : conversation.getUltimoEntranteEn();
        if (lastInbound != null && OffsetDateTime.now(java.time.ZoneOffset.UTC)
                .isBefore(lastInbound.plusHours(CUSTOMER_SERVICE_WINDOW_HOURS))) {
            // El cliente escribio: la ventana esta abierta y el vendedor puede responder
            // en texto libre. Gastar una plantilla aqui seria pagar de mas y molestar.
            return "El cliente respondio y la ventana de 24 horas esta abierta";
        }
        return null;
    }

    private void handleFailure(CrmWhatsappReengagementOutbox job, RuntimeException error) {
        int attempts = job.getAttempts() == null ? 1 : job.getAttempts();
        boolean definitive = error instanceof BusinessException business
                && NON_RETRYABLE.contains(business.getCode());
        boolean exhausted = definitive || attempts >= MAX_ATTEMPTS;

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
                "Reenganche {} fallo en el intento {} (tenant={}): {}",
                job.getId(),
                attempts,
                job.getTenantId(),
                exhausted ? "sin mas reintentos" : "se reintentara"
        );
    }

    private void resolve(CrmWhatsappReengagementOutbox job, String status, String resultado) {
        int updated = outboxRepository.markResolved(
                job.getId(), workerId, status, trim(resultado, 500), LocalDateTime.now());
        if (updated != 1) {
            log.error("El reenganche {} termino sin conservar la propiedad del lease", job.getId());
        }
    }

    private void reschedule(CrmWhatsappReengagementOutbox job, ZonedDateTime opening) {
        LocalDateTime next = opening.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();
        outboxRepository.markFailedAttempt(
                job.getId(),
                workerId,
                "RETRY",
                next,
                "Fuera del horario de envio; reprogramado para " + next,
                now
        );
    }

    private ZoneId tenantZone() {
        try {
            return autoReplyConfigurationService.tenantZone();
        } catch (RuntimeException error) {
            log.warn("No se pudo resolver la zona horaria del tenant; se usa la del servidor", error);
            return ZoneId.systemDefault();
        }
    }

    boolean insideBusinessHours(ZonedDateTime moment) {
        return businessDayNumbers().contains(moment.getDayOfWeek().getValue())
                && !moment.toLocalTime().isBefore(startTime())
                && moment.toLocalTime().isBefore(endTime());
    }

    private LocalTime startTime() {
        return LocalTime.of(Math.max(0, Math.min(23, businessHourStart)), 0);
    }

    /** Un cierre en 24 significa "hasta el final del dia", que LocalTime.of no admite. */
    private LocalTime endTime() {
        return businessHourEnd >= 24 ? LocalTime.MAX : LocalTime.of(Math.max(0, businessHourEnd), 0);
    }

    ZonedDateTime nextOpening(ZonedDateTime from) {
        Set<Integer> days = businessDayNumbers();
        LocalTime start = startTime();
        if (days.contains(from.getDayOfWeek().getValue()) && from.toLocalTime().isBefore(start)) {
            return from.with(start);
        }
        ZonedDateTime candidate = from.plusDays(1).with(start);
        for (int day = 0; day < 7; day++) {
            if (days.contains(candidate.getDayOfWeek().getValue())) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        // Sin dias habiles configurados no hay franja a la que correrlo: se revisa en una hora.
        return from.plusHours(1);
    }

    private Set<Integer> businessDayNumbers() {
        Set<Integer> days = java.util.Arrays.stream(businessDays.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException error) {
                        return -1;
                    }
                })
                .filter(value -> value >= DayOfWeek.MONDAY.getValue() && value <= DayOfWeek.SUNDAY.getValue())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return days.isEmpty() ? Set.of(1, 2, 3, 4, 5, 6) : days;
    }

    private void restoreTenant(String previousTenant) {
        if (TenantContext.DEFAULT_TENANT.equals(previousTenant)) {
            TenantContext.clear();
        } else {
            TenantContext.setTenantId(previousTenant);
        }
    }

    private String trimError(RuntimeException error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return trim(message, 1000);
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
