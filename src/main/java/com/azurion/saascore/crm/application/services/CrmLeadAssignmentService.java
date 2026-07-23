package com.azurion.saascore.crm.application.services;

import com.azurion.saascore.crm.application.dto.CrmLeadAssignmentConfigResponse;
import com.azurion.saascore.crm.application.dto.UpdateCrmLeadAssignmentConfigRequest;
import com.azurion.saascore.crm.domain.entities.CrmLeadAssignmentConfig;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmLeadAssignmentConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CrmLeadAssignmentService {

    private static final String DEFAULT_CONFIG = "DEFAULT";
    private static final String STRATEGY = "MENOR_CARGA";

    private final CrmLeadAssignmentConfigRepository configRepository;
    private final CrmProspectoRepository prospectoRepository;

    @Transactional(readOnly = true)
    public CrmLeadAssignmentConfigResponse getConfiguration() {
        return configRepository.findByCodigo(DEFAULT_CONFIG)
                .map(this::toResponse)
                .orElseGet(() -> new CrmLeadAssignmentConfigResponse(false, STRATEGY, List.of()));
    }

    @Transactional
    public CrmLeadAssignmentConfigResponse updateConfiguration(UpdateCrmLeadAssignmentConfigRequest request) {
        List<String> responsableIds = normalizeIds(request.responsableIds());
        if (Boolean.TRUE.equals(request.automatico()) && responsableIds.isEmpty()) {
            throw new BusinessException(
                    "CRM_VENDEDORES_REQUERIDOS",
                    "Selecciona al menos un vendedor para activar el reparto automatico"
            );
        }

        CrmLeadAssignmentConfig config = configRepository.findByCodigoForUpdate(DEFAULT_CONFIG)
                .orElseGet(CrmLeadAssignmentConfig::new);
        config.setCodigo(DEFAULT_CONFIG);
        config.setAutomatico(Boolean.TRUE.equals(request.automatico()));
        config.setEstrategia(STRATEGY);
        config.setResponsableIds(serialize(responsableIds));
        return toResponse(configRepository.save(config));
    }

    @Transactional
    public String assignAutomatically(CrmProspecto prospecto, String fallbackOwner) {
        CrmLeadAssignmentConfig config = configRepository.findByCodigoForUpdate(DEFAULT_CONFIG).orElse(null);
        if (config == null || !config.isAutomatico()) {
            prospecto.setResponsableId(fallbackOwner);
            return fallbackOwner;
        }

        List<String> responsableIds = parse(config.getResponsableIds());
        if (responsableIds.isEmpty()) {
            prospecto.setResponsableId(fallbackOwner);
            return fallbackOwner;
        }

        String selected = responsableIds.stream()
                .min(Comparator
                        .comparingLong((String id) -> prospectoRepository.countByResponsableIdAndEstado(id, "NUEVO"))
                        .thenComparingInt(responsableIds::indexOf))
                .orElse(fallbackOwner);
        prospecto.setResponsableId(selected);
        return selected;
    }

    private CrmLeadAssignmentConfigResponse toResponse(CrmLeadAssignmentConfig config) {
        return new CrmLeadAssignmentConfigResponse(
                config.isAutomatico(),
                config.getEstrategia(),
                parse(config.getResponsableIds())
        );
    }

    private List<String> normalizeIds(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> value == null ? null : value.trim())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String serialize(List<String> values) {
        return String.join("\n", values);
    }

    private List<String> parse(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return normalizeIds(value.lines().toList());
    }
}
