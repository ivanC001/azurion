package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.CrmWhatsappReengagementResponse;
import com.azurion.saascore.crm.application.dto.ScheduleQuoteReengagementRequest;
import com.azurion.saascore.crm.application.dto.ScheduleWhatsappReengagementRequest;
import com.azurion.saascore.crm.domain.WhatsappTemplate;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappReengagementOutbox;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappReengagementOutboxRepository;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.shared.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Programa y cancela los reenganches de WhatsApp.
 *
 * <p>Fuera de la ventana de 24 horas Meta solo acepta plantillas aprobadas, asi que
 * este servicio valida contra el catalogo del WABA en el momento de programar: es
 * preferible que el usuario se entere en ese instante y no una semana despues,
 * cuando el worker intente enviar.
 */
@Service
@RequiredArgsConstructor
public class WhatsappReengagementService {

    /** Debajo de este margen no tiene sentido diferir: se envia desde la conversacion. */
    static final long MIN_SCHEDULE_MINUTES = 5;
    private static final long MAX_SCHEDULE_DAYS = 365;
    /** Estados en los que el cliente ya vio la cotizacion. */
    private static final java.util.Set<String> ESTADOS_CITABLES =
            java.util.Set.of("ENVIADA", "VENCIDA");

    private final CrmWhatsappReengagementOutboxRepository outboxRepository;
    private final CrmProspectoRepository prospectoRepository;
    private final WhatsappIntegrationService whatsappIntegrationService;
    private final CotizacionRepository cotizacionRepository;
    private final CotizacionReengagementParameterBuilder parameterBuilder;
    private final ObjectMapper objectMapper;

    @Transactional
    public CrmWhatsappReengagementResponse schedule(
            Long prospectoId,
            ScheduleWhatsappReengagementRequest request) {
        CrmProspecto prospecto = prospectoRepository.findById(prospectoId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_PROSPECTO_NO_ENCONTRADO",
                        "El prospecto solicitado no existe"
                ));
        if (prospecto.getWhatsappOptoutEn() != null) {
            throw new BusinessException(
                    "CRM_WHATSAPP_PROSPECTO_DIO_DE_BAJA",
                    "El prospecto pidio no recibir mas mensajes de WhatsApp."
            );
        }

        LocalDateTime scheduledAt = request.programadoPara();
        LocalDateTime now = LocalDateTime.now();
        if (scheduledAt.isBefore(now.plusMinutes(MIN_SCHEDULE_MINUTES))) {
            throw new BusinessException(
                    "CRM_WHATSAPP_REENGANCHE_FECHA_INVALIDA",
                    "Programa el reenganche al menos " + MIN_SCHEDULE_MINUTES
                            + " minutos hacia adelante."
            );
        }
        if (scheduledAt.isAfter(now.plusDays(MAX_SCHEDULE_DAYS))) {
            throw new BusinessException(
                    "CRM_WHATSAPP_REENGANCHE_FECHA_INVALIDA",
                    "No se puede programar un reenganche a mas de un ano."
            );
        }

        // Valida nombre, idioma y cantidad de parametros contra el WABA.
        WhatsappTemplate template = whatsappIntegrationService.requireSendableTemplate(
                request.nombre(),
                request.idioma()
        );
        List<String> parametros = template.validateParameters(request.parametros());

        String tenantId = TenantContext.getTenantId();
        String dedupeKey = dedupeKey(prospectoId, template, scheduledAt);
        outboxRepository.findByTenantIdAndDedupeKey(tenantId, dedupeKey).ifPresent(existing -> {
            throw new BusinessException(
                    "CRM_WHATSAPP_REENGANCHE_DUPLICADO",
                    "Ya existe un reenganche con esa plantilla y esa fecha para este prospecto."
            );
        });

        CrmWhatsappReengagementOutbox job = new CrmWhatsappReengagementOutbox();
        job.setTenantId(tenantId);
        job.setProspectoId(prospectoId);
        job.setDedupeKey(dedupeKey);
        job.setPlantillaNombre(template.name());
        job.setPlantillaIdioma(template.languageCode());
        job.setParametrosJson(writeParameters(parametros));
        job.setScheduledAt(scheduledAt);
        job.setNextAttemptAt(scheduledAt);
        job.setStatus("PENDING");
        job.setCreadoPor(currentUser());
        return toResponse(outboxRepository.save(job));
    }

    /**
     * Programa un reenganche citando una cotizacion del prospecto y llenando las
     * variables de la plantilla con sus datos, sin escribirlas a mano.
     */
    @Transactional
    public CrmWhatsappReengagementResponse scheduleFromQuote(
            Long prospectoId,
            ScheduleQuoteReengagementRequest request) {
        CrmProspecto prospecto = prospectoRepository.findById(prospectoId)
                .orElseThrow(() -> new BusinessException(
                        "CRM_PROSPECTO_NO_ENCONTRADO",
                        "El prospecto solicitado no existe"
                ));
        Cotizacion cotizacion = resolveQuote(prospectoId, request.cotizacionId());
        List<String> parametros =
                parameterBuilder.build(prospecto, cotizacion, request.campos());

        return schedule(prospectoId, new ScheduleWhatsappReengagementRequest(
                request.nombre(),
                request.idioma(),
                parametros,
                request.programadoPara()
        ));
    }

    private Cotizacion resolveQuote(Long prospectoId, Long cotizacionId) {
        if (cotizacionId != null) {
            Cotizacion cotizacion = cotizacionRepository
                    .findByIdAndCrmProspectoId(cotizacionId, prospectoId)
                    .orElseThrow(() -> new BusinessException(
                            "CRM_COTIZACION_NO_ENCONTRADA",
                            "La cotizacion no existe o no pertenece a este prospecto"
                    ));
            return requireVista(cotizacion);
        }

        // Sin id explicito se toma la ultima que el cliente llego a ver. La consulta ya
        // viene ordenada por fecha de emision descendente.
        return cotizacionRepository.findAllByCrmProspectoId(prospectoId).stream()
                .filter(quote -> ESTADOS_CITABLES.contains(quote.getEstado()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "CRM_COTIZACION_NO_ENCONTRADA",
                        "El prospecto no tiene ninguna cotizacion enviada o vencida que citar."
                ));
    }

    /**
     * Una cotizacion en BORRADOR nunca salio del CRM. Citarla le hablaria al cliente de
     * algo que no vio, y de paso invalidaria el encuadre de utilidad de la plantilla.
     */
    private Cotizacion requireVista(Cotizacion cotizacion) {
        if (!ESTADOS_CITABLES.contains(cotizacion.getEstado())) {
            throw new BusinessException(
                    "CRM_COTIZACION_NO_CITABLE",
                    "Solo se puede citar una cotizacion que el cliente ya recibio. "
                            + "Esta esta en estado " + cotizacion.getEstado() + "."
            );
        }
        return cotizacion;
    }

    @Transactional(readOnly = true)
    public List<CrmWhatsappReengagementResponse> listForProspecto(Long prospectoId) {
        return outboxRepository
                .findAllByTenantIdAndProspectoIdOrderByScheduledAtDesc(TenantContext.getTenantId(), prospectoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public int cancelForProspecto(Long prospectoId, String motivo) {
        return outboxRepository.cancelPendingForProspecto(
                TenantContext.getTenantId(),
                prospectoId,
                motivo,
                LocalDateTime.now()
        );
    }

    List<String> readParameters(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Los parametros persistidos del reenganche no son validos", exception);
        }
    }

    private String writeParameters(List<String> parametros) {
        try {
            return objectMapper.writeValueAsString(parametros);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudieron serializar los parametros del reenganche", exception);
        }
    }

    private String dedupeKey(Long prospectoId, WhatsappTemplate template, LocalDateTime scheduledAt) {
        String key = prospectoId + ":" + template.name().toLowerCase(Locale.ROOT)
                + ":" + template.languageCode().toLowerCase(Locale.ROOT)
                + ":" + scheduledAt.truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        return truncate(key, 180);
    }

    CrmWhatsappReengagementResponse toResponse(CrmWhatsappReengagementOutbox job) {
        return new CrmWhatsappReengagementResponse(
                job.getId(),
                job.getProspectoId(),
                job.getPlantillaNombre(),
                job.getPlantillaIdioma(),
                readParameters(job.getParametrosJson()),
                job.getScheduledAt(),
                job.getStatus(),
                job.getAttempts(),
                job.getResultado(),
                job.getLastError(),
                job.getProcessedAt(),
                job.getCreadoPor()
        );
    }

    private String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || !authentication.isAuthenticated()
                ? "sistema"
                : truncate(authentication.getName(), 120);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
