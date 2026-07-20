package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.SendCotizacionEmailResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.settings.email.application.services.EmailAttachment;
import com.azurion.saascore.settings.email.application.services.EmailSenderService;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class SendCotizacionEmailUseCase {

    private final GetCotizacionUseCase getCotizacionUseCase;
    private final GenerateCotizacionPdfUseCase generateCotizacionPdfUseCase;
    private final UpdateCotizacionEstadoUseCase updateCotizacionEstadoUseCase;
    private final CrmOportunidadRepository oportunidadRepository;
    private final EmailSenderService emailSenderService;
    private final PlatformTransactionManager transactionManager;
    private final CotizacionRepository cotizacionRepository;

    public SendCotizacionEmailResponse execute(Long id) {
        String sendToken = UUID.randomUUID().toString();
        claimSend(id, sendToken);
        EmailContent content;
        boolean smtpStarted = false;
        try {
            content = prepareEmail(id);
            CotizacionPdfResponse pdf = generateCotizacionPdfUseCase.execute(id);

            smtpStarted = true;
            emailSenderService.sendEmail(
                    TenantContext.getTenantId(),
                    content.destinatario(),
                    content.subject(),
                    content.body(),
                    List.of(new EmailAttachment(
                            pdf.fileName(),
                            pdf.contentType(),
                            Base64.getDecoder().decode(pdf.base64().getBytes(StandardCharsets.UTF_8))
                    ))
            );
            if (cotizacionRepository.markEmailSent(id, sendToken, LocalDateTime.now()) != 1) {
                throw BusinessException.internal("COTIZACION_EMAIL_LEASE_LOST", "No se pudo confirmar el envio de la cotizacion.");
            }
        } catch (RuntimeException error) {
            if (smtpStarted) {
                cotizacionRepository.markEmailUncertain(id, sendToken, trimError(error), LocalDateTime.now());
            } else {
                cotizacionRepository.markEmailFailed(id, sendToken, trimError(error), LocalDateTime.now());
            }
            throw error;
        }

        CotizacionResponse updated = updateCotizacionEstadoUseCase.execute(
                id,
                new UpdateCotizacionEstadoRequest("ENVIADA", "CORREO", null, null, null)
        );
        return new SendCotizacionEmailResponse(updated, content.destinatario());
    }

    private void claimSend(Long id, String sendToken) {
        if (cotizacionRepository.claimEmailSend(id, sendToken, OffsetDateTime.now(), LocalDateTime.now()) == 1) {
            return;
        }
        Cotizacion quote = getCotizacionUseCase.find(id);
        if ("SENT".equals(quote.getEmailSendStatus())) {
            throw new BusinessException("COTIZACION_EMAIL_YA_ENVIADO", "La cotizacion ya fue enviada por correo.");
        }
        if ("UNKNOWN".equals(quote.getEmailSendStatus())) {
            throw BusinessException.conflict(
                    "COTIZACION_EMAIL_ESTADO_INCIERTO",
                    "El envio anterior tiene un resultado incierto y debe revisarse antes de reenviar."
            );
        }
        throw new BusinessException("COTIZACION_EMAIL_EN_PROCESO", "La cotizacion ya se esta enviando. Espera la confirmacion.");
    }

    private EmailContent prepareEmail(Long id) {
        EmailContent content = new TransactionTemplate(transactionManager).execute(status -> {
            Cotizacion cotizacion = getCotizacionUseCase.find(id);
            return new EmailContent(resolveRecipient(cotizacion), subject(cotizacion), body(cotizacion));
        });
        if (content == null) {
            throw BusinessException.internal("COTIZACION_EMAIL_PREPARATION_ERROR", "No se pudo preparar el correo de cotizacion.");
        }
        return content;
    }

    private String trimError(RuntimeException error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    private String resolveRecipient(Cotizacion cotizacion) {
        String clientEmail = email(cotizacion.getCliente());
        if (clientEmail != null) {
            return clientEmail;
        }
        if (cotizacion.getCrmOportunidadId() != null) {
            CrmOportunidad oportunidad = oportunidadRepository.findWithRelationsById(cotizacion.getCrmOportunidadId())
                    .orElseThrow(() -> new BusinessException(
                            "COTIZACION_OPORTUNIDAD_NO_ENCONTRADA",
                            "No se encontro la oportunidad vinculada a la cotizacion."
                    ));
            String opportunityClientEmail = email(oportunidad.getCliente());
            if (opportunityClientEmail != null) {
                return opportunityClientEmail;
            }
            CrmProspecto prospecto = oportunidad.getProspecto();
            if (prospecto != null && hasText(prospecto.getCorreo())) {
                return prospecto.getCorreo().trim();
            }
        }
        throw new BusinessException(
                "COTIZACION_DESTINATARIO_SIN_CORREO",
                "El cliente o prospecto no tiene un correo registrado para recibir la cotizacion."
        );
    }

    private String email(Cliente cliente) {
        return cliente != null && hasText(cliente.getEmail()) ? cliente.getEmail().trim() : null;
    }

    private String subject(Cotizacion cotizacion) {
        return "Cotizacion COT-" + String.format("%03d", cotizacion.getId());
    }

    private String body(Cotizacion cotizacion) {
        String nombre = cotizacion.getCliente() != null && hasText(cotizacion.getCliente().getNombre())
                ? cotizacion.getCliente().getNombre().trim()
                : "cliente";
        return "Hola " + nombre + ",\n\n"
                + "Adjuntamos la cotizacion COT-" + String.format("%03d", cotizacion.getId())
                + " por un total de S/ " + money(cotizacion.getTotal()) + ".\n\n"
                + "Quedamos atentos a tus comentarios.\n";
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2).toPlainString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record EmailContent(String destinatario, String subject, String body) {
    }
}
