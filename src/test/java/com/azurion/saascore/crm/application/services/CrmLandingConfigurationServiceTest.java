package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.azurion.saascore.crm.application.dto.CrmLandingConfigResponse;
import com.azurion.saascore.crm.application.dto.SaveCrmLandingConfigRequest;
import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.LandingProductMode;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingCatalogItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingConfigRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrmLandingConfigurationServiceTest {

    @Mock
    private CrmLandingConfigRepository landingConfigRepository;
    @Mock
    private CrmLandingCatalogItemRepository landingCatalogItemRepository;
    @Mock
    private CrmCatalogoItemRepository catalogoItemRepository;

    private CrmLandingConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new CrmLandingConfigurationService(
                landingConfigRepository,
                landingCatalogItemRepository,
                catalogoItemRepository
        );
        when(landingConfigRepository.save(any(CrmLandingConfig.class))).thenAnswer(invocation -> {
            CrmLandingConfig landing = invocation.getArgument(0);
            landing.setId(10L);
            return landing;
        });
        when(landingCatalogItemRepository.findAllByLandingConfigOrderByIdAsc(any(CrmLandingConfig.class)))
                .thenReturn(List.of());
    }

    @Test
    void createsServerGeneratedKeyWithOptionalProductByDefault() {
        when(landingConfigRepository.existsByLandingKey(any())).thenReturn(false);

        CrmLandingConfigResponse response = service.create(new SaveCrmLandingConfigRequest(
                "Landing Municipios",
                "municipios",
                null,
                true,
                true,
                true,
                null,
                List.of()
        ));

        assertNotNull(response.landingKey());
        assertTrue(response.landingKey().startsWith("lnd_"));
        assertTrue(response.landingKey().length() >= 40);
        assertEquals(LandingProductMode.OPCIONAL, response.modoProducto());
        assertEquals("municipios", response.campania());
    }
}
