package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.dto.CrmWhatsappMessageResponse;
import com.azurion.saascore.crm.application.dto.WhatsappAutoReplyConfigResponse;
import com.azurion.saascore.crm.application.dto.WhatsappAutoReplyScheduleResponse;
import com.azurion.saascore.crm.application.events.WhatsappInboundMessageStoredEvent;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappAutoReplyDispatch;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappMessage;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappAutoReplyDispatchRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappConversationRepository;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappMessageRepository;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappAutoReplyProcessor {

    private static final Set<String> ACTIVE_STATES = Set.of("PENDIENTE", "PROCESANDO");

    private final CrmWhatsappAutoReplyDispatchRepository dispatchRepository;
    private final CrmWhatsappConversationRepository conversationRepository;
    private final CrmWhatsappMessageRepository messageRepository;
    private final WhatsappAutoReplyConfigurationService configurationService;
    private final WhatsappIntegrationService whatsappIntegrationService;
    private final PlatformTransactionManager transactionManager;

    @EventListener
    public void onInboundMessage(WhatsappInboundMessageStoredEvent event) {
        String previousTenant = TenantContext.getTenantId();
        TenantContext.setTenantId(event.tenantId());
        try {
            DispatchWork work = inNewTransaction(() -> claim(event.dispatchId()));
            if (work == null) {
                return;
            }
            CrmWhatsappMessageResponse outgoing = whatsappIntegrationService.sendAutomaticMessage(
                    work.prospectoId(),
                    work.message()
            );
            inNewTransaction(() -> {
                CrmWhatsappAutoReplyDispatch dispatch = dispatchRepository.findById(event.dispatchId()).orElse(null);
                if (dispatch != null) {
                    dispatch.setEstado("ENVIADO");
                    dispatch.setDetalle("Respuesta automatica enviada por Meta");
                    dispatch.setProcessedAt(OffsetDateTime.now());
                    messageRepository.findById(outgoing.id()).ifPresent(dispatch::setOutgoingMessage);
                    dispatchRepository.save(dispatch);
                }
                return null;
            });
        } catch (RuntimeException exception) {
            log.error("Fallo la respuesta automatica de WhatsApp dispatch={}", event.dispatchId(), exception);
            try {
                inNewTransaction(() -> {
                    dispatchRepository.findById(event.dispatchId()).ifPresent(dispatch -> {
                        dispatch.setEstado("ERROR");
                        dispatch.setDetalle(truncate(exception.getMessage(), 500));
                        dispatch.setProcessedAt(OffsetDateTime.now());
                        dispatchRepository.save(dispatch);
                    });
                    return null;
                });
            } catch (RuntimeException markError) {
                log.error("No se pudo auditar el error de respuesta automatica dispatch={}", event.dispatchId(), markError);
            }
        } finally {
            if (TenantContext.DEFAULT_TENANT.equals(previousTenant)) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
    }

    private DispatchWork claim(Long dispatchId) {
        CrmWhatsappAutoReplyDispatch dispatch = dispatchRepository.findWithDetailsById(dispatchId).orElse(null);
        if (dispatch == null || !"PENDIENTE".equals(dispatch.getEstado())) {
            return null;
        }
        Long prospectoId = dispatch.getProspecto().getId();
        conversationRepository.findForUpdateByProspectoId(prospectoId);

        WhatsappAutoReplyConfigResponse config = configurationService.getConfiguration();
        if (!config.activo() || config.mensaje() == null || config.mensaje().isBlank()) {
            omit(dispatch, "La respuesta automatica esta desactivada");
            return null;
        }
        if (dispatchRepository.existsByProspecto_IdAndEstadoInAndIdNot(prospectoId, ACTIVE_STATES, dispatchId)) {
            omit(dispatch, "Ya existe una respuesta automatica en proceso para esta conversacion");
            return null;
        }
        if ("HORARIO".equals(config.modo()) && !isInsideSchedule(config)) {
            omit(dispatch, "Mensaje recibido fuera del horario configurado");
            return null;
        }

        CrmWhatsappMessage latest = messageRepository
                .findFirstByProspecto_IdAndEnviadoPorUsuarioIdOrderByMensajeEnDescIdDesc(
                        prospectoId,
                        WhatsappIntegrationService.AUTOMATIC_WHATSAPP_OWNER
                )
                .orElse(null);
        if (latest != null && latest.getMensajeEn() != null
                && latest.getMensajeEn().plusMinutes(config.cooldownMinutos()).isAfter(OffsetDateTime.now())) {
            omit(dispatch, "Conversacion dentro del tiempo de espera configurado");
            return null;
        }

        dispatch.setEstado("PROCESANDO");
        dispatch.setDetalle("Preparando respuesta automatica");
        dispatchRepository.save(dispatch);
        return new DispatchWork(prospectoId, config.mensaje());
    }

    private boolean isInsideSchedule(WhatsappAutoReplyConfigResponse config) {
        ZonedDateTime now = ZonedDateTime.now(configurationService.tenantZone());
        int today = now.getDayOfWeek().getValue();
        int yesterday = now.minusDays(1).getDayOfWeek().getValue();
        LocalTime time = now.toLocalTime();
        for (WhatsappAutoReplyScheduleResponse schedule : config.horarios()) {
            if (!schedule.activo()) {
                continue;
            }
            LocalTime start = schedule.horaInicio();
            LocalTime end = schedule.horaFin();
            if (start.isBefore(end)
                    && schedule.diaSemana() == today
                    && !time.isBefore(start)
                    && time.isBefore(end)) {
                return true;
            }
            if (start.isAfter(end)) {
                if (schedule.diaSemana() == today && !time.isBefore(start)) {
                    return true;
                }
                if (schedule.diaSemana() == yesterday && time.isBefore(end)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void omit(CrmWhatsappAutoReplyDispatch dispatch, String detail) {
        dispatch.setEstado("OMITIDO");
        dispatch.setDetalle(detail);
        dispatch.setProcessedAt(OffsetDateTime.now());
        dispatchRepository.save(dispatch);
    }

    private <T> T inNewTransaction(java.util.concurrent.Callable<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> {
            try {
                return action.call();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private String truncate(String value, int max) {
        String normalized = value == null || value.isBlank() ? "Error no especificado" : value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private record DispatchWork(Long prospectoId, String message) {
    }
}
