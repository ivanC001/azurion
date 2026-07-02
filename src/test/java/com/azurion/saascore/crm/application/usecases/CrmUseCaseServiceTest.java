package com.azurion.saascore.crm.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.clientes.application.usecases.CreateClienteUseCase;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.cotizaciones.application.usecases.CreateCotizacionUseCase;
import com.azurion.saascore.crm.application.dto.CreateCrmProspectoRequest;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmEtapaPipelineRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadHistorialRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CrmUseCaseServiceTest {

    @Mock
    CrmProspectoRepository prospectoRepository;

    @Mock
    CrmCatalogoItemRepository catalogoItemRepository;

    @Mock
    CrmOportunidadRepository oportunidadRepository;

    @Mock
    CrmActividadRepository actividadRepository;

    @Mock
    CrmEtapaPipelineRepository etapaPipelineRepository;

    @Mock
    CrmOportunidadHistorialRepository historialRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    CreateClienteUseCase createClienteUseCase;

    @Mock
    CreateCotizacionUseCase createCotizacionUseCase;

    @Mock
    AuthorizationService authorizationService;

    CrmUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new CrmUseCaseService(
                prospectoRepository,
                catalogoItemRepository,
                oportunidadRepository,
                actividadRepository,
                etapaPipelineRepository,
                historialRepository,
                clienteRepository,
                createClienteUseCase,
                createCotizacionUseCase,
                authorizationService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void vendedorNoPuedeAsignarProspectoAOtroResponsable() {
        authenticate("CRM_WRITE");
        when(authorizationService.currentUsuarioId()).thenReturn(10L);

        CreateCrmProspectoRequest request = new CreateCrmProspectoRequest(
                "NATURAL",
                "1",
                "12345678",
                "Cliente interesado",
                null,
                null,
                null,
                "cliente@test.local",
                null,
                "WEB",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "20",
                null
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createProspecto(request));

        assertEquals("CRM_ASIGNACION_NO_PERMITIDA", exception.getCode());
        verify(prospectoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioConPermisoAssignPuedeAsignarProspecto() {
        authenticate("CRM_WRITE", "CRM_ASSIGN");
        when(authorizationService.currentUsuarioId()).thenReturn(10L);
        when(prospectoRepository.save(org.mockito.ArgumentMatchers.any(CrmProspecto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateCrmProspectoRequest request = new CreateCrmProspectoRequest(
                "NATURAL",
                "1",
                "12345678",
                "Cliente interesado",
                null,
                null,
                null,
                "cliente@test.local",
                null,
                "WEB",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "20",
                null
        );

        service.createProspecto(request);

        ArgumentCaptor<CrmProspecto> captor = ArgumentCaptor.forClass(CrmProspecto.class);
        verify(prospectoRepository).save(captor.capture());
        assertEquals("20", captor.getValue().getResponsableId());
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario",
                "n/a",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        ));
    }
}
