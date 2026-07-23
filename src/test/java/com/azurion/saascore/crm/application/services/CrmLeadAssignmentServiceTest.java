package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.UpdateCrmLeadAssignmentConfigRequest;
import com.azurion.saascore.crm.domain.entities.CrmLeadAssignmentConfig;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmLeadAssignmentConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrmLeadAssignmentServiceTest {

    @Mock
    private CrmLeadAssignmentConfigRepository configRepository;
    @Mock
    private CrmProspectoRepository prospectoRepository;

    private CrmLeadAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new CrmLeadAssignmentService(configRepository, prospectoRepository);
    }

    @Test
    void assignsNewLeadToSellerWithLowestLoad() {
        CrmLeadAssignmentConfig config = new CrmLeadAssignmentConfig();
        config.setAutomatico(true);
        config.setResponsableIds("20\n30");
        when(configRepository.findByCodigoForUpdate("DEFAULT")).thenReturn(Optional.of(config));
        when(prospectoRepository.countByResponsableIdAndEstado("20", "NUEVO")).thenReturn(5L);
        when(prospectoRepository.countByResponsableIdAndEstado("30", "NUEVO")).thenReturn(2L);
        CrmProspecto prospecto = new CrmProspecto();

        String selected = service.assignAutomatically(prospecto, "crm-public");

        assertEquals("30", selected);
        assertEquals("30", prospecto.getResponsableId());
    }

    @Test
    void keepsPublicQueueWhenAutomaticAssignmentIsDisabled() {
        CrmLeadAssignmentConfig config = new CrmLeadAssignmentConfig();
        config.setAutomatico(false);
        when(configRepository.findByCodigoForUpdate("DEFAULT")).thenReturn(Optional.of(config));
        CrmProspecto prospecto = new CrmProspecto();

        service.assignAutomatically(prospecto, "crm-public");

        assertEquals("crm-public", prospecto.getResponsableId());
    }

    @Test
    void rejectsAutomaticModeWithoutSellers() {
        BusinessException error = assertThrows(BusinessException.class, () ->
                service.updateConfiguration(new UpdateCrmLeadAssignmentConfigRequest(true, List.of()))
        );

        assertEquals("CRM_VENDEDORES_REQUERIDOS", error.getCode());
    }
}
