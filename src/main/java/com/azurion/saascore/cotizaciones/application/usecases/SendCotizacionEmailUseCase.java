package com.azurion.saascore.cotizaciones.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.SendCotizacionEmailResponse;
import com.azurion.saascore.cotizaciones.application.dto.UpdateCotizacionEstadoRequest;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SendCotizacionEmailUseCase {

    private final GetCotizacionUseCase getCotizacionUseCase;
    private final GenerateCotizacionPdfUseCase generateCotizacionPdfUseCase;
    private final UpdateCotizacionEstadoUseCase updateCotizacionEstadoUseCase;
    private final CrmOportunidadRepository oportunidadRepository;
    private final EmailSenderService emailSenderService;

    @Transactional
    public SendCotizacionEmailResponse execute(Long id) {
        Cotizacion cotizacion = getCotizacionUseCase.find(id);
        String destinatario = resolveRecipient(cotizacion);
        CotizacionPdfResponse pdf = generateCotizacionPdfUseCase.execute(id);

        emailSenderService.sendEmail(
                TenantContext.getTenantId(),
                destinatario,
                subject(cotizacion),
                body(cotizacion),
                List.of(new EmailAttachment(
                        pdf.fileName(),
                        pdf.contentType(),
                        Base64.getDecoder().decode(pdf.base64().getBytes(StandardCharsets.UTF_8))
                ))
        );

        CotizacionResponse updated = updateCotizacionEstadoUseCase.execute(
                id,
                new UpdateCotizacionEstadoRequest("ENVIADA", "CORREO", null, null, null)
        );
        return new SendCotizacionEmailResponse(updated, destinatario);
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
}
