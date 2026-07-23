package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingCatalogItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingConfigRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LandingLeadValidationServiceTest {

    @Mock
    CrmCatalogoItemRepository catalogoItemRepository;

    @Mock
    CrmCanalTokenConfigRepository canalTokenConfigRepository;

    @Mock
    CrmLandingConfigRepository landingConfigRepository;

    @Mock
    CrmLandingCatalogItemRepository landingCatalogItemRepository;

    LandingLeadValidationService service;

    @BeforeEach
    void setUp() {
        service = new LandingLeadValidationService(
                catalogoItemRepository,
                canalTokenConfigRepository,
                landingConfigRepository,
                landingCatalogItemRepository
        );
    }

    @Test
    void rechazaLeadCuandoCanalWebEstaDesactivado() {
        CrmCanalTokenConfig web = new CrmCanalTokenConfig();
        web.setCanal("WEB");
        web.setActivo(false);
        when(canalTokenConfigRepository.findByCanal("WEB")).thenReturn(Optional.of(web));

        BusinessException error = assertThrows(BusinessException.class, () -> service.validate(validRequest()));

        assertEquals("CRM_CANAL_WEB_INACTIVO", error.getCode());
    }

    @Test
    void aceptaContratoDirectoCuandoCanalYProductoEstanActivos() {
        CrmCanalTokenConfig web = new CrmCanalTokenConfig();
        web.setCanal("WEB");
        web.setActivo(true);
        CrmCatalogoItem item = new CrmCatalogoItem();
        item.setId(2L);
        item.setEstado("ACTIVO");
        item.setPublicEnabled(true);
        when(canalTokenConfigRepository.findByCanal("WEB")).thenReturn(Optional.of(web));
        when(catalogoItemRepository.findByIdAndPublicToken(2L, "TOKEN_DEL_CURSO"))
                .thenReturn(Optional.of(item));

        var context = service.validate(validRequest());

        assertEquals(2L, context.catalogoItem().getId());
        assertEquals("LANDING", context.canalIngreso());
        assertEquals("municipios", context.campania());
    }

    private PublicCrmLeadRequest validRequest() {
        return new PublicCrmLeadRequest(
                "20000000012",
                null,
                null,
                null,
                null,
                "Juan Perez",
                null,
                "juan@perez.com",
                "999999999",
                null,
                null,
                "LANDING",
                "municipios",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                2L,
                "TOKEN_DEL_CURSO",
                "",
                null
        );
    }
}
