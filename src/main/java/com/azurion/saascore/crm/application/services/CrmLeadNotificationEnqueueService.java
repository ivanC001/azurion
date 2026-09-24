package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.events.CrmLeadNotificationQueuedEvent;
import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationConfig;
import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationDispatch;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationDispatchRepository;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Encola el aviso por correo dentro de la transaccion que registro el lead y lo
 * publica recien despues del commit.
 *
 * El orden importa: si el correo se mandara en linea, un SMTP caido tumbaria el
 * registro del lead que entro por la landing o por WhatsApp. El lead siempre gana.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmLeadNotificationEnqueueService {

    public static final String TIPO_LEAD_NUEVO = "LEAD_NUEVO";
    public static final String TIPO_MENSAJE_NUEVO = "MENSAJE_NUEVO";

    private static final int MAX_DESTINATARIOS_LENGTH = 1000;
    private static final int MAX_ASUNTO_LENGTH = 300;

    private final CrmLeadNotificationConfigRepository configRepository;
    private final CrmLeadNotificationDispatchRepository dispatchRepository;
    private final UsuarioTenantRepository usuarioTenantRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Nunca propaga errores: un fallo avisando no puede invalidar el lead recien
     * registrado, que es el dato que de verdad importa.
     */
    public void enqueue(CrmProspecto prospecto, String tipo, String asunto) {
        try {
            enqueueInternal(prospecto, tipo, asunto);
        } catch (RuntimeException exception) {
            log.error("No se pudo encolar el aviso {} del prospecto {}", tipo, prospecto == null ? null : prospecto.getId(), exception);
        }
    }

    private void enqueueInternal(CrmProspecto prospecto, String tipo, String asunto) {
        if (prospecto == null || prospecto.getId() == null) {
            return;
        }
        CrmLeadNotificationConfig config = configRepository.findFirstByOrderByIdAsc().orElse(null);
        if (config == null || !config.isActivo() || !isTipoHabilitado(config, tipo)) {
            return;
        }
        if (config.getCooldownMinutos() > 0 && dispatchRepository.existsRecent(
                prospecto.getId(),
                tipo,
                LocalDateTime.now().minusMinutes(config.getCooldownMinutos()))) {
            return;
        }

        List<String> destinatarios = resolveRecipients(config, prospecto);
        if (destinatarios.isEmpty()) {
            return;
        }

        CrmLeadNotificationDispatch dispatch = new CrmLeadNotificationDispatch();
        dispatch.setProspecto(prospecto);
        dispatch.setTipo(tipo);
        dispatch.setDestinatarios(truncate(String.join(",", destinatarios), MAX_DESTINATARIOS_LENGTH));
        dispatch.setAsunto(truncate(asunto, MAX_ASUNTO_LENGTH));
        dispatch.setEstado("PENDIENTE");
        CrmLeadNotificationDispatch saved = dispatchRepository.save(dispatch);
        publishAfterCommit(new CrmLeadNotificationQueuedEvent(TenantContext.getTenantId(), saved.getId()));
    }

    private boolean isTipoHabilitado(CrmLeadNotificationConfig config, String tipo) {
        return TIPO_LEAD_NUEVO.equals(tipo) ? config.isNotificarLeadNuevo() : config.isNotificarMensajeNuevo();
    }

    /**
     * El asesor asignado primero y los correos de supervision despues. El duenio
     * ficticio de los leads publicos ("crm-public") no es un usuario real.
     */
    private List<String> resolveRecipients(CrmLeadNotificationConfig config, CrmProspecto prospecto) {
        Set<String> recipients = new LinkedHashSet<>();
        if (config.isNotificarResponsable()) {
            advisorEmail(prospecto.getResponsableId()).ifPresent(recipients::add);
        }
        recipients.addAll(parseEmails(config.getCorreosCopia()));
        return new ArrayList<>(recipients);
    }

    private java.util.Optional<String> advisorEmail(String responsableId) {
        if (responsableId == null || responsableId.isBlank()) {
            return java.util.Optional.empty();
        }
        String trimmed = responsableId.trim();
        java.util.Optional<UsuarioTenant> usuario = parseId(trimmed)
                .flatMap(usuarioTenantRepository::findById)
                .or(() -> usuarioTenantRepository.findByUsernameAndActivoTrue(trimmed));
        return usuario
                .filter(UsuarioTenant::isActivo)
                .map(UsuarioTenant::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase(Locale.ROOT));
    }

    private java.util.Optional<Long> parseId(String value) {
        try {
            return java.util.Optional.of(Long.valueOf(value));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }

    private List<String> parseEmails(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split("[,;\s]+"))
                .map(String::trim)
                .filter(value -> value.contains("@"))
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private void publishAfterCommit(CrmLeadNotificationQueuedEvent event) {
        Runnable publish = () -> {
            try {
                eventPublisher.publishEvent(event);
            } catch (RuntimeException exception) {
                log.error("No se pudo publicar el aviso de lead dispatch={}", event.dispatchId(), exception);
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

    private String truncate(String value, int max) {
        String safe = value == null ? "" : value.trim();
        return safe.length() <= max ? safe : safe.substring(0, max);
    }
}
