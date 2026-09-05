package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CrmWhatsappFailedSendResponse;
import com.azurion.saascore.crm.domain.WhatsappDeliveryFailureCatalog;
import com.azurion.saascore.crm.domain.entities.CrmWhatsappMessage;
import com.azurion.saascore.crm.domain.repositories.CrmWhatsappMessageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de los envios de WhatsApp que Meta no entrego.
 *
 * <p>El motivo de un fallo llega por webhook segundos despues del envio, cuando el
 * usuario ya cerro la pantalla: el mensaje aparece como enviado y recien despues pasa
 * a fallido. Hasta ahora eso solo se veia abriendo la conversacion del prospecto, asi
 * que un problema de cuenta —una tarjeta sin cargar, por ejemplo— podia tumbar todos
 * los envios sin que nadie supiera por que.
 */
@Service
@RequiredArgsConstructor
public class WhatsappDeliveryFailureService {

    private static final int MAX_REGISTROS = 20;

    private final CrmWhatsappMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<CrmWhatsappFailedSendResponse> recentFailures() {
        return messageRepository
                .findAllByDireccionAndEstadoOrderByMensajeEnDescIdDesc(
                        "SALIENTE", "FALLIDO", PageRequest.of(0, MAX_REGISTROS))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CrmWhatsappFailedSendResponse toResponse(CrmWhatsappMessage message) {
        var explicacion = WhatsappDeliveryFailureCatalog.explicar(message.getErrorCodigo());
        return new CrmWhatsappFailedSendResponse(
                message.getId(),
                message.getProspecto() == null ? null : message.getProspecto().getId(),
                message.getProspecto() == null ? null : message.getProspecto().getNombre(),
                message.getTipoMensaje(),
                message.getPlantillaNombre(),
                message.getErrorCodigo(),
                explicacion.map(WhatsappDeliveryFailureCatalog.Explicacion::causa)
                        .orElse("Meta no pudo entregar el mensaje."),
                explicacion.map(WhatsappDeliveryFailureCatalog.Explicacion::solucion).orElse(null),
                message.getErrorDetalle(),
                message.getMensajeEn()
        );
    }
}
