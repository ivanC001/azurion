package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CreateWhatsappTemplateRequest;
import com.azurion.saascore.crm.application.dto.CrmWhatsappTemplateDraftResponse;
import com.azurion.saascore.crm.domain.WhatsappTemplateDraft;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.infrastructure.http.WhatsappCloudApiClient;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja que cada empresa arme sus propias plantillas de WhatsApp desde el CRM.
 *
 * <p>Antes habia que crearlas en el Administrador de WhatsApp de Meta, sin ninguna
 * pista de que componentes soporta el compositor: se podia aprobar una plantilla con
 * encabezado de imagen o botones con variables y descubrir recien al enviarla que el
 * CRM no la podia usar. Al componerla aca esas reglas se validan antes de mandarla a
 * revision, asi que lo que Meta aprueba es siempre enviable.
 */
@Service
@RequiredArgsConstructor
public class WhatsappTemplateAuthoringService {

    private final CrmCanalTokenConfigRepository configRepository;
    private final WhatsappCloudApiClient cloudApiClient;

    @Transactional(readOnly = true)
    public CrmWhatsappTemplateDraftResponse create(CreateWhatsappTemplateRequest request) {
        WhatsappCloudApiClient.CreatedTemplate created =
                cloudApiClient.createTemplate(requireActiveConfig(), toDraft(request));

        return new CrmWhatsappTemplateDraftResponse(
                created.id(),
                created.name(),
                created.languageCode(),
                created.category(),
                created.status(),
                mensaje(created.status())
        );
    }

    private WhatsappTemplateDraft toDraft(CreateWhatsappTemplateRequest request) {
        return new WhatsappTemplateDraft(
                request.nombre(),
                request.idioma(),
                request.categoria(),
                new WhatsappTemplateDraft.Component(
                        request.encabezado(),
                        request.ejemploEncabezado() == null ? List.of() : request.ejemploEncabezado()
                ),
                new WhatsappTemplateDraft.Component(
                        request.cuerpo(),
                        request.ejemploCuerpo() == null ? List.of() : request.ejemploCuerpo()
                ),
                request.pie(),
                request.botones() == null ? List.of() : request.botones().stream()
                        .map(boton -> new WhatsappTemplateDraft.Button(
                                boton.tipo(), boton.texto(), boton.url(), boton.telefono()))
                        .toList()
        );
    }

    private String mensaje(String estado) {
        if ("APPROVED".equalsIgnoreCase(estado)) {
            return "Meta aprobo la plantilla: ya se puede usar para reenganchar.";
        }
        if ("REJECTED".equalsIgnoreCase(estado)) {
            return "Meta rechazo la plantilla. Revisa el motivo en el Administrador de WhatsApp.";
        }
        return "La plantilla quedo en revision. Cuando Meta la apruebe aparece sola en la lista, "
                + "sin tocar nada mas.";
    }

    private CrmCanalTokenConfig requireActiveConfig() {
        CrmCanalTokenConfig config = configRepository.findByCanal("WHATSAPP")
                .orElseThrow(() -> new BusinessException(
                        "CRM_WHATSAPP_NO_CONFIGURADO",
                        "WhatsApp no esta configurado para este tenant"
                ));
        if (!config.isActivo()) {
            throw new BusinessException(
                    "CRM_WHATSAPP_INACTIVO",
                    "La integracion de WhatsApp esta inactiva"
            );
        }
        return config;
    }
}
