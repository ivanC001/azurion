package com.azurion.saascore.crm.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.azurion.saascore.crm.application.dto.CrmLandingConfigResponse;
import com.azurion.saascore.crm.application.dto.SaveCrmLandingConfigRequest;
import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.LandingProductMode;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingCatalogItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmLandingConfigRepository;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
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
    @Mock
    private CrmLandingIngressRegistryService ingressRegistryService;
    @Mock
    private UsuarioTenantRepository usuarioTenantRepository;

    private CrmLandingConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new CrmLandingConfigurationService(
                landingConfigRepository,
                landingCatalogItemRepository,
                catalogoItemRepository,
                ingressRegistryService,
                usuarioTenantRepository
        );
        lenient().when(landingConfigRepository.save(any(CrmLandingConfig.class))).thenAnswer(invocation -> {
            CrmLandingConfig landing = invocation.getArgument(0);
            landing.setId(10L);
            return landing;
        });
        when(landingCatalogItemRepository.findAllByLandingConfigOrderByIdAsc(any(CrmLandingConfig.class)))
                .thenReturn(List.of());
        when(ingressRegistryService.synchronize(any(CrmLandingConfig.class), anyBoolean()))
                .thenAnswer(invocation -> new CrmLandingIngressRegistryService.LandingIngressCredentials(
                        "lnd_test",
                        invocation.getArgument(1) ? "rls_test" : null
                ));
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
                "TELEFONO_CORREO",
                List.of()
        ));

        assertNotNull(response.landingKey());
        assertTrue(response.landingKey().startsWith("lnd_"));
        assertTrue(response.landingKey().length() >= 40);
        assertEquals(LandingProductMode.OPCIONAL, response.modoProducto());
        assertEquals("municipios", response.campania());
        assertEquals("TELEFONO_CORREO", response.validarDuplicadosPor());
        assertEquals("rls_test", response.relaySecret());
    }

    @Test
    void listDoesNotRevealExistingRelaySecrets() {
        CrmLandingConfig landing = new CrmLandingConfig();
        landing.setId(12L);
        landing.setNombre("Landing segura");
        landing.setLandingKey("lnd_segura");
        when(landingConfigRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(landing));

        CrmLandingConfigResponse response = service.list().getFirst();

        assertNull(response.relaySecret());
        assertEquals("TELEFONO_CORREO", response.validarDuplicadosPor());
    }
}
