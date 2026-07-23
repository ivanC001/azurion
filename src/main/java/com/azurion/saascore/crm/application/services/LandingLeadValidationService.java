package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.LandingProductMode;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingCatalogItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingConfigRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LandingLeadValidationService {

    private final CrmCatalogoItemRepository catalogoItemRepository;
    private final CrmCanalTokenConfigRepository canalTokenConfigRepository;
    private final CrmLandingConfigRepository landingConfigRepository;
    private final CrmLandingCatalogItemRepository landingCatalogItemRepository;

    public LandingLeadContext validate(PublicCrmLeadRequest request) {
        validateAntispam(request);
        validateWebChannelEnabled();
        validateContact(request);

        String landingKey = trim(request.landingKey());
        if (landingKey == null) {
            CrmCatalogoItem item = findPublicCatalogoItem(request.catalogoItemId(), request.catalogoToken());
            return new LandingLeadContext(
                    null,
                    item,
                    false,
                    true,
                    firstNonBlank(request.canalIngreso(), "LANDING"),
                    trim(request.campania()),
                    null
            );
        }

        CrmLandingConfig landing = landingConfigRepository.findByLandingKey(landingKey)
                .orElseThrow(() -> reject("CRM_LANDING_KEY_INVALIDA", "Landing key invalida", landingKey));
        if (!landing.isActiva() || !landing.isRecibirLeads()) {
            throw reject("CRM_LANDING_INACTIVA", "La landing no esta habilitada para recibir leads", landingKey);
        }

        LandingProductMode mode = landing.getModoProducto() == null ? LandingProductMode.REQUERIDO : landing.getModoProducto();
        CrmCatalogoItem catalogoItem = switch (mode) {
            case REQUERIDO -> validateRequiredProduct(request, landing);
            case OPCIONAL -> request.catalogoItemId() == null ? null : validateOptionalProduct(request, landing);
            case SIN_CATALOGO -> null;
        };

        boolean productPending = catalogoItem == null;
        return new LandingLeadContext(
                landing,
                catalogoItem,
                productPending,
                landing.isCrearActividadInicial(),
                firstNonBlank(request.canalIngreso(), landing.getCanalIngreso(), "LANDING"),
                firstNonBlank(request.campania(), landing.getCampania()),
                trim(landing.getResponsableId())
        );
    }

    private void validateWebChannelEnabled() {
        boolean enabled = canalTokenConfigRepository.findByCanal("WEB")
                .map(CrmCanalTokenConfig::isActivo)
                .orElse(false);
        if (!enabled) {
            throw new BusinessException(
                    "CRM_CANAL_WEB_INACTIVO",
                    "La recepcion de leads web esta desactivada en Configuracion CRM"
            );
        }
    }

    private CrmCatalogoItem validateRequiredProduct(PublicCrmLeadRequest request, CrmLandingConfig landing) {
        if (request.catalogoItemId() == null || trim(request.catalogoToken()) == null) {
            throw reject("CRM_LANDING_PRODUCTO_REQUERIDO", "La landing requiere producto y token de catalogo", landing.getLandingKey());
        }
        CrmCatalogoItem item = findPublicCatalogoItem(request.catalogoItemId(), request.catalogoToken());
        ensureProductAllowed(landing, item.getId());
        return item;
    }

    private CrmCatalogoItem validateOptionalProduct(PublicCrmLeadRequest request, CrmLandingConfig landing) {
        CrmCatalogoItem item = trim(request.catalogoToken()) == null
                ? findPublicCatalogoItemById(request.catalogoItemId())
                : findPublicCatalogoItem(request.catalogoItemId(), request.catalogoToken());
        ensureProductAllowed(landing, item.getId());
        return item;
    }

    private void ensureProductAllowed(CrmLandingConfig landing, Long catalogoItemId) {
        if (!landingCatalogItemRepository.existsByLandingConfigAndCatalogoItem_IdAndActivoTrue(landing, catalogoItemId)) {
            throw reject("CRM_LANDING_PRODUCTO_NO_PERMITIDO", "El producto no esta permitido para esta landing", landing.getLandingKey());
        }
    }

    private CrmCatalogoItem findPublicCatalogoItem(Long id, String publicToken) {
        if (id == null || trim(publicToken) == null) {
            throw new BusinessException("CRM_CATALOGO_PUBLICO_REQUERIDO", "La landing debe enviar una oferta CRM valida");
        }
        CrmCatalogoItem item = catalogoItemRepository.findByIdAndPublicToken(id, publicToken.trim())
                .orElseThrow(() -> new BusinessException("CRM_CATALOGO_PUBLICO_INVALIDO", "La oferta CRM no es valida para esta landing"));
        ensurePublicCatalogoItemActive(item);
        return item;
    }

    private CrmCatalogoItem findPublicCatalogoItemById(Long id) {
        if (id == null) {
            throw new BusinessException("CRM_CATALOGO_PUBLICO_REQUERIDO", "La landing debe enviar una oferta CRM valida");
        }
        CrmCatalogoItem item = catalogoItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CRM_CATALOGO_PUBLICO_INVALIDO", "La oferta CRM no es valida para esta landing"));
        ensurePublicCatalogoItemActive(item);
        return item;
    }

    private void ensurePublicCatalogoItemActive(CrmCatalogoItem item) {
        if (!item.isPublicEnabled() || !"ACTIVO".equals(item.getEstado())) {
            throw new BusinessException("CRM_CATALOGO_PUBLICO_INACTIVO", "La oferta CRM no esta disponible para captar leads");
        }
    }

    private void validateAntispam(PublicCrmLeadRequest request) {
        if (trim(request.website()) != null) {
            log.warn("Lead publico rechazado por honeypot website con tenant={} landingKey={}", mask(request.rucTenant()), mask(request.landingKey()));
            throw new BusinessException("CRM_LEAD_PUBLICO_RECHAZADO", "El lead no pudo ser validado");
        }
    }

    private void validateContact(PublicCrmLeadRequest request) {
        if (trim(request.nombre()) == null) {
            throw new BusinessException("CRM_LEAD_NOMBRE_REQUERIDO", "El nombre del lead es obligatorio");
        }
        if (trim(request.telefono()) == null && trim(request.correo()) == null) {
            throw new BusinessException("CRM_LEAD_CONTACTO_REQUERIDO", "Debe enviar telefono o correo");
        }
    }

    private BusinessException reject(String code, String message, String landingKey) {
        log.warn("Lead publico rechazado: {} landingKey={}", code, mask(landingKey));
        return new BusinessException(code, message);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trim(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String mask(String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.length() <= 4) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 2);
    }

    public record LandingLeadContext(
            CrmLandingConfig landingConfig,
            CrmCatalogoItem catalogoItem,
            boolean productoPendiente,
            boolean crearActividadInicial,
            String canalIngreso,
            String campania,
            String responsableId
    ) {
    }
}
