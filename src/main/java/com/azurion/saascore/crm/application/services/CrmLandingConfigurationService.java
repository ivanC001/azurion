package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CrmLandingConfigResponse;
import com.azurion.saascore.crm.application.dto.SaveCrmLandingConfigRequest;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.entities.CrmLandingCatalogItem;
import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.LandingProductMode;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingCatalogItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingConfigRepository;
import com.azurion.shared.exception.BusinessException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrmLandingConfigurationService {

    private static final SecureRandom KEY_RANDOM = new SecureRandom();

    private final CrmLandingConfigRepository landingConfigRepository;
    private final CrmLandingCatalogItemRepository landingCatalogItemRepository;
    private final CrmCatalogoItemRepository catalogoItemRepository;

    @Transactional(readOnly = true)
    public List<CrmLandingConfigResponse> list() {
        return landingConfigRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CrmLandingConfigResponse create(SaveCrmLandingConfigRequest request) {
        CrmLandingConfig landing = new CrmLandingConfig();
        landing.setLandingKey(generateLandingKey());
        apply(landing, request);
        CrmLandingConfig saved = landingConfigRepository.save(landing);
        syncCatalogItems(saved, request.catalogoItemIds());
        return toResponse(saved);
    }

    @Transactional
    public CrmLandingConfigResponse update(Long id, SaveCrmLandingConfigRequest request) {
        CrmLandingConfig landing = findLanding(id);
        apply(landing, request);
        CrmLandingConfig saved = landingConfigRepository.save(landing);
        syncCatalogItems(saved, request.catalogoItemIds());
        return toResponse(saved);
    }

    @Transactional
    public CrmLandingConfigResponse regenerateKey(Long id) {
        CrmLandingConfig landing = findLanding(id);
        landing.setLandingKey(generateLandingKey());
        return toResponse(landingConfigRepository.save(landing));
    }

    private void apply(CrmLandingConfig landing, SaveCrmLandingConfigRequest request) {
        landing.setNombre(request.nombre().trim());
        landing.setCampania(trim(request.campania()));
        landing.setCanalIngreso("LANDING");
        landing.setModoProducto(request.modoProducto() == null ? LandingProductMode.OPCIONAL : request.modoProducto());
        landing.setActiva(request.activa() == null || request.activa());
        landing.setRecibirLeads(request.recibirLeads() == null || request.recibirLeads());
        landing.setCrearSeguimiento(true);
        landing.setCrearActividadInicial(request.crearActividadInicial() == null || request.crearActividadInicial());
        landing.setResponsableId(trim(request.responsableId()));
        landing.setValidarDuplicadosPor("TELEFONO_CORREO");
    }

    private void syncCatalogItems(CrmLandingConfig landing, List<Long> requestedIds) {
        Set<Long> desiredIds = new LinkedHashSet<>();
        if (requestedIds != null) {
            requestedIds.stream().filter(id -> id != null && id > 0).forEach(desiredIds::add);
        }

        List<CrmLandingCatalogItem> existing = landingCatalogItemRepository.findAllByLandingConfigOrderByIdAsc(landing);
        for (CrmLandingCatalogItem relation : existing) {
            if (desiredIds.remove(relation.getCatalogoItem().getId())) {
                relation.setActivo(true);
                landingCatalogItemRepository.save(relation);
            } else {
                landingCatalogItemRepository.delete(relation);
            }
        }

        if (desiredIds.isEmpty()) {
            return;
        }

        List<CrmCatalogoItem> catalogItems = new ArrayList<>();
        catalogoItemRepository.findAllById(desiredIds).forEach(catalogItems::add);
        if (catalogItems.size() != desiredIds.size()) {
            throw new BusinessException("CRM_LANDING_CATALOGO_INVALIDO", "Uno o mas productos seleccionados no existen");
        }
        for (CrmCatalogoItem item : catalogItems) {
            if (!item.isPublicEnabled() || !"ACTIVO".equals(item.getEstado())) {
                throw new BusinessException("CRM_LANDING_CATALOGO_INACTIVO", "Solo puedes vincular productos publicos y activos");
            }
            CrmLandingCatalogItem relation = new CrmLandingCatalogItem();
            relation.setLandingConfig(landing);
            relation.setCatalogoItem(item);
            relation.setActivo(true);
            landingCatalogItemRepository.save(relation);
        }
    }

    private CrmLandingConfigResponse toResponse(CrmLandingConfig landing) {
        List<Long> catalogItemIds = landingCatalogItemRepository.findAllByLandingConfigOrderByIdAsc(landing).stream()
                .filter(CrmLandingCatalogItem::isActivo)
                .map(relation -> relation.getCatalogoItem().getId())
                .toList();
        return new CrmLandingConfigResponse(
                landing.getId(),
                landing.getNombre(),
                landing.getLandingKey(),
                landing.getCampania(),
                landing.getCanalIngreso(),
                landing.isActiva(),
                landing.isRecibirLeads(),
                landing.getModoProducto(),
                landing.isCrearActividadInicial(),
                landing.getResponsableId(),
                catalogItemIds,
                landing.getCreatedAt(),
                landing.getUpdatedAt()
        );
    }

    private CrmLandingConfig findLanding(Long id) {
        return landingConfigRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CRM_LANDING_NO_ENCONTRADA", "La configuracion de landing no existe"));
    }

    private String generateLandingKey() {
        String key;
        do {
            byte[] bytes = new byte[32];
            KEY_RANDOM.nextBytes(bytes);
            key = "lnd_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (landingConfigRepository.existsByLandingKey(key));
        return key;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
