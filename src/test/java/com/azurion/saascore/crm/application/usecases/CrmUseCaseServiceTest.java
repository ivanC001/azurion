package com.azurion.saascore.crm.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.application.usecases.CreateClienteUseCase;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.cotizaciones.application.usecases.CreateCotizacionUseCase;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.application.dto.CreateCrmProspectoRequest;
import com.azurion.saascore.crm.application.dto.RepartirCrmProspectosRequest;
import com.azurion.saascore.crm.application.dto.RepartirCrmProspectosResponse;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmEtapaPipelineRepository;
import com.azurion.saascore.crm.domain.repositories.CrmNegociacionRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadHistorialRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
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
    CrmNegociacionRepository negociacionRepository;

    @Mock
    CrmOportunidadHistorialRepository historialRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    CreateClienteUseCase createClienteUseCase;

    @Mock
    CreateCotizacionUseCase createCotizacionUseCase;

    @Mock
    CotizacionRepository cotizacionRepository;

    @Mock
    AuthorizationService authorizationService;

    @Mock
    CrmCanalTokenConfigRepository canalTokenConfigRepository;

    CrmUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new CrmUseCaseService(
                prospectoRepository,
                catalogoItemRepository,
                oportunidadRepository,
                actividadRepository,
                etapaPipelineRepository,
                negociacionRepository,
                historialRepository,
                clienteRepository,
                createClienteUseCase,
                createCotizacionUseCase,
                cotizacionRepository,
                authorizationService,
                canalTokenConfigRepository
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

        CreateCrmProspectoRequest request = prospectoRequestAsignadoA("20");

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

        CreateCrmProspectoRequest request = prospectoRequestAsignadoA("20");

        service.createProspecto(request);

        ArgumentCaptor<CrmProspecto> captor = ArgumentCaptor.forClass(CrmProspecto.class);
        verify(prospectoRepository).save(captor.capture());
        assertEquals("20", captor.getValue().getResponsableId());
    }

    @Test
    void vendedorNoPuedeRepartirProspectos() {
        authenticate("CRM_LEADS_WRITE");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.repartirProspectos(
                new RepartirCrmProspectosRequest(List.of(1L), List.of("20"), true)
        ));

        assertEquals("CRM_ASIGNACION_NO_PERMITIDA", exception.getCode());
        verify(prospectoRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void administradorReparteProspectosNuevosEquitativamente() {
        authenticate("CRM_ASSIGN");
        CrmProspecto first = prospecto(1L);
        CrmProspecto second = prospecto(2L);
        CrmProspecto third = prospecto(3L);
        when(prospectoRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(first, second, third));
        when(prospectoRepository.countByResponsableIdAndEstado("20", "NUEVO")).thenReturn(0L);
        when(prospectoRepository.countByResponsableIdAndEstado("30", "NUEVO")).thenReturn(1L);
        when(prospectoRepository.saveAll(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RepartirCrmProspectosResponse response = service.repartirProspectos(
                new RepartirCrmProspectosRequest(List.of(1L, 2L, 3L), List.of("20", "30"), true)
        );

        assertEquals(3, response.totalAsignados());
        assertEquals("20", first.getResponsableId());
        assertEquals("20", second.getResponsableId());
        assertEquals("30", third.getResponsableId());
    }

    @Test
    void prospectoConvertidoAOportunidadTodaviaPuedeConvertirseACliente() {
        authenticate("ROLE_ADMIN");
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona("NATURAL");
        prospecto.setTipoDocumento("1");
        prospecto.setNumeroDocumento("74859621");
        prospecto.setNombre("Cliente cierre CRM");
        prospecto.setEstado("CONVERTIDO");
        prospecto.setResponsableId("10");
        when(prospectoRepository.findById(17L)).thenReturn(Optional.of(prospecto));
        when(createClienteUseCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(new ClienteResponse(
                8L,
                "1",
                "74859621",
                "Cliente cierre CRM",
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                false,
                true
        ));
        when(prospectoRepository.save(prospecto)).thenReturn(prospecto);

        ClienteResponse cliente = service.convertirProspectoCliente(17L);

        assertEquals(8L, cliente.id());
        assertEquals(8L, prospecto.getClienteId());
        assertEquals("CONVERTIDO", prospecto.getEstado());
        assertNotNull(prospecto.getFechaConversion());
        verify(prospectoRepository).save(prospecto);
    }

    @Test
    void prospectoSeEnlazaAClienteExistentePorDocumento() {
        authenticate("ROLE_ADMIN");
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona("NATURAL");
        prospecto.setTipoDocumento("1");
        prospecto.setNumeroDocumento("74859621");
        prospecto.setNombre("Cliente cierre CRM");
        prospecto.setEstado("CONVERTIDO");
        prospecto.setResponsableId("10");

        Cliente existing = new Cliente();
        existing.setId(8L);
        existing.setTipoDocumento("1");
        existing.setNumeroDocumento("74859621");
        existing.setNombre("Cliente cierre CRM");
        existing.setLimiteCredito(BigDecimal.ZERO);
        existing.setSaldoDeuda(BigDecimal.ZERO);
        existing.setDiasCredito(0);
        existing.setActivo(true);

        when(prospectoRepository.findById(17L)).thenReturn(Optional.of(prospecto));
        when(clienteRepository.findByTipoDocumentoAndNumeroDocumento("1", "74859621")).thenReturn(Optional.of(existing));
        when(prospectoRepository.save(prospecto)).thenReturn(prospecto);

        ClienteResponse cliente = service.convertirProspectoCliente(17L);

        assertEquals(8L, cliente.id());
        assertEquals(8L, prospecto.getClienteId());
        assertEquals("CONVERTIDO", prospecto.getEstado());
        assertNotNull(prospecto.getFechaConversion());
        verify(createClienteUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
        verify(prospectoRepository).save(prospecto);
    }

    private CreateCrmProspectoRequest prospectoRequestAsignadoA(String responsableId) {
        return new CreateCrmProspectoRequest(
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                responsableId,
                null
        );
    }

    private CrmProspecto prospecto(Long id) {
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setId(id);
        prospecto.setTipoPersona("NATURAL");
        prospecto.setNombre("Lead " + id);
        prospecto.setOrigen("WEB");
        prospecto.setCanalIngreso("LANDING");
        prospecto.setTipoInteres("CURSO");
        prospecto.setEstado("NUEVO");
        prospecto.setResponsableId("crm-public");
        return prospecto;
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario",
                "n/a",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        ));
    }
}
