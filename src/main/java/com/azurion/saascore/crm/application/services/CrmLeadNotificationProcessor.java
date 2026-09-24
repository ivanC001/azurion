package com.azurion.saascore.crm.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.crm.application.events.CrmLeadNotificationQueuedEvent;
import com.azurion.saascore.crm.domain.entities.CrmLeadNotificationDispatch;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmLeadNotificationDispatchRepository;
import com.azurion.saascore.settings.email.application.services.EmailSenderService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envia el aviso por correo usando el SMTP del propio tenant.
 *
 * Corre despues del commit del lead, repone el tenant en el contexto (el listener
 * puede ejecutarse fuera del hilo de la peticion) y deja el resultado auditado:
 * ENVIADO, ERROR con el motivo, u OMITIDO si el tenant no tiene correo verificado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmLeadNotificationProcessor {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_DETALLE = 500;

    private final CrmLeadNotificationDispatchRepository dispatchRepository;
    private final EmailSenderService emailSenderService;
    private final PlatformTransactionManager transactionManager;

    @EventListener
    public void onNotificationQueued(CrmLeadNotificationQueuedEvent event) {
        String previousTenant = TenantContext.getTenantId();
        TenantContext.setTenantId(event.tenantId());
        try {
            NotificationWork work = inNewTransaction(() -> claim(event.dispatchId()));
            if (work == null) {
                return;
            }
            for (String recipient : work.destinatarios()) {
                emailSenderService.sendEmail(event.tenantId(), recipient, work.asunto(), work.cuerpo(), List.of());
            }
            markProcessed(event.dispatchId(), "ENVIADO",
                    "Aviso enviado a " + work.destinatarios().size() + " destinatario(s)");
        } catch (RuntimeException exception) {
            log.warn("No se pudo enviar el aviso de lead dispatch={}", event.dispatchId(), exception);
            // Sin SMTP verificado el aviso se omite: es configuracion pendiente del
            // tenant, no un fallo del CRM.
            String estado = isMissingEmailConfig(exception) ? "OMITIDO" : "ERROR";
            markProcessed(event.dispatchId(), estado, exception.getMessage());
        } finally {
            if (previousTenant == null || TenantContext.DEFAULT_TENANT.equals(previousTenant)) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(previousTenant);
            }
        }
    }

    private NotificationWork claim(Long dispatchId) {
        CrmLeadNotificationDispatch dispatch = dispatchRepository.findById(dispatchId).orElse(null);
        if (dispatch == null || !"PENDIENTE".equals(dispatch.getEstado())) {
            return null;
        }
        List<String> destinatarios = Arrays.stream(dispatch.getDestinatarios().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (destinatarios.isEmpty()) {
            dispatch.setEstado("OMITIDO");
            dispatch.setDetalle("El aviso no tenia destinatarios");
            dispatch.setProcessedAt(LocalDateTime.now());
            dispatchRepository.save(dispatch);
            return null;
        }
        return new NotificationWork(destinatarios, dispatch.getAsunto(), buildBody(dispatch));
    }

    private String buildBody(CrmLeadNotificationDispatch dispatch) {
        CrmProspecto prospecto = dispatch.getProspecto();
        boolean esLeadNuevo = CrmLeadNotificationEnqueueService.TIPO_LEAD_NUEVO.equals(dispatch.getTipo());
        StringBuilder body = new StringBuilder();
        body.append(esLeadNuevo
                ? "Entro un lead nuevo al CRM.\n\n"
                : "Un prospecto en seguimiento envio un mensaje nuevo.\n\n");
        body.append("Contacto: ").append(defaultText(prospecto.getNombre(), "Sin nombre")).append("\n");
        body.append("Telefono: ").append(defaultText(prospecto.getTelefono(), "No registrado")).append("\n");
        body.append("Correo: ").append(defaultText(prospecto.getCorreo(), "No registrado")).append("\n");
        body.append("Canal: ").append(defaultText(
                firstNonBlank(prospecto.getCanalIngreso(), prospecto.getOrigen()), "No registrado")).append("\n");
        body.append("Interes: ").append(defaultText(prospecto.getInteresPrincipal(), "Por confirmar")).append("\n");
        if (hasText(prospecto.getMensaje())) {
            body.append("\nMensaje del cliente:\n").append(prospecto.getMensaje().trim()).append("\n");
        }
        body.append("\nRecibido el ").append(FECHA.format(
                prospecto.getUpdatedAt() == null ? LocalDateTime.now() : prospecto.getUpdatedAt()));
        body.append("\n\nAtiendelo desde el CRM de Azurion para no perder la oportunidad.\n");
        return body.toString();
    }

    private void markProcessed(Long dispatchId, String estado, String detalle) {
        try {
            inNewTransaction(() -> {
                dispatchRepository.findById(dispatchId).ifPresent(dispatch -> {
                    dispatch.setEstado(estado);
                    dispatch.setDetalle(truncate(detalle));
                    dispatch.setProcessedAt(LocalDateTime.now());
                    dispatchRepository.save(dispatch);
                });
                return null;
            });
        } catch (RuntimeException exception) {
            log.error("No se pudo auditar el aviso de lead dispatch={}", dispatchId, exception);
        }
    }

    private boolean isMissingEmailConfig(RuntimeException exception) {
        String message = exception.getMessage();
        return message != null && (message.contains("EMAIL_CONFIG") || message.contains("correo"));
    }

    private <T> T inNewTransaction(Callable<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> {
            try {
                return action.call();
            } catch (RuntimeException runtimeException) {
                throw runtimeException;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private String truncate(String value) {
        String safe = value == null ? "" : value.trim();
        return safe.length() <= MAX_DETALLE ? safe : safe.substring(0, MAX_DETALLE);
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String firstNonBlank(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record NotificationWork(List<String> destinatarios, String asunto, String cuerpo) {
    }
}
