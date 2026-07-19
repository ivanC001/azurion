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
import com.azurion.saascore.crm.application.services.LandingLeadValidationService;
import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.repositories.CrmActividadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCanalTokenConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.crm.domain.repositories.CrmCurrencyConfigRepository;
import com.azurion.saascore.crm.domain.repositories.CrmEtapaPipelineRepository;
import com.azurion.saascore.crm.domain.repositories.CrmNegociacionRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadHistorialRepository;
import com.azurion.saascore.crm.domain.repositories.CrmOportunidadRepository;
import com.azurion.saascore.crm.domain.repositories.CrmProspectoInteresRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

    @Mock
    CrmCurrencyConfigRepository currencyConfigRepository;

    @Mock
    LandingLeadValidationService landingLeadValidationService;

    @Mock
    CrmProspectoInteresRepository prospectoInteresRepository;

    @Mock
    CrmSecretEncryptionService crmSecretEncryptionService;

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
                canalTokenConfigRepository,
                currencyConfigRepository,
                landingLeadValidationService,
                prospectoInteresRepository,
                crmSecretEncryptionService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void vendedorNoPuedeAsignarProspectoAOtroResponsable() {
        authenticate("CRM_LEADS_WRITE");
        when(authorizationService.currentUsuarioId()).thenReturn(10L);

        CreateCrmProspectoRequest request = prospectoRequestAsignadoA("20");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createProspecto(request));

        assertEquals("CRM_ASIGNACION_NO_PERMITIDA", exception.getCode());
        verify(prospectoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioConPermisoAssignPuedeAsignarProspecto() {
        authenticate("CRM_LEADS_WRITE", "CRM_ASSIGN");
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
    void bandejaSoloExponeCanalesActivosDelTenant() {
        CrmCanalTokenConfig whatsapp = new CrmCanalTokenConfig();
        whatsapp.setCanal("WHATSAPP");
        whatsapp.setNombre("WhatsApp Business");
        whatsapp.setActivo(true);
        CrmCanalTokenConfig facebook = new CrmCanalTokenConfig();
        facebook.setCanal("FACEBOOK");
        facebook.setNombre("Facebook Lead Ads");
        facebook.setActivo(false);
        when(canalTokenConfigRepository.findAllByOrderByCanalAsc()).thenReturn(List.of(facebook, whatsapp));

        var channels = service.listInboxChannels(true);

        assertEquals(List.of("WHATSAPP", "CORREO"), channels.stream()
                .filter(item -> item.activo())
                .map(item -> item.canal())
                .toList());
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
        authenticate("CRM_VIEW_ALL");
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona("NATURAL");
        prospecto.setTipoDocumento("1");
        prospecto.setNumeroDocumento("74859621");
        prospecto.setNombre("Cliente cierre CRM");
        prospecto.setEstado("CONVERTIDO");
        prospecto.setResponsableId("10");
        Cliente createdCliente = new Cliente();
        createdCliente.setId(8L);
        createdCliente.setTipoDocumento("1");
        createdCliente.setNumeroDocumento("74859621");
        createdCliente.setNombre("Cliente cierre CRM");
        createdCliente.setLimiteCredito(BigDecimal.ZERO);
        createdCliente.setSaldoDeuda(BigDecimal.ZERO);
        createdCliente.setDiasCredito(0);
        createdCliente.setActivo(true);
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
        when(clienteRepository.findById(8L)).thenReturn(Optional.of(createdCliente));
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
        authenticate("CRM_VIEW_ALL");
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

    @Test
    void pipelineSoloExponeEtapasActivasDeTrabajoYOportunidadesAbiertas() {
        authenticate("CRM_VIEW_ALL");
        CrmEtapaPipeline interesado = etapa(1L, "INTERESADO", 1, false, false);
        CrmEtapaPipeline cotizado = etapa(2L, "COTIZADO", 2, false, false);
        CrmEtapaPipeline negociacion = etapa(3L, "NEGOCIACION", 3, false, false);
        CrmEtapaPipeline ganado = etapa(4L, "GANADO", 4, true, false);
        CrmEtapaPipeline perdido = etapa(5L, "PERDIDO", 5, false, true);

        CrmOportunidad abierta = oportunidad(10L, interesado, "ABIERTA");
        CrmOportunidad cerrada = oportunidad(11L, ganado, "GANADA");
        when(etapaPipelineRepository.findByActivoTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(interesado, cotizado, negociacion, ganado, perdido));
        when(oportunidadRepository.findAllByOrderByIdDesc()).thenReturn(List.of(abierta, cerrada));

        var pipeline = service.pipeline();

        assertEquals(List.of("INTERESADO", "COTIZADO", "NEGOCIACION"),
                pipeline.stream().map(column -> column.etapa().codigo()).toList());
        assertEquals(1, pipeline.getFirst().cantidad());
        assertEquals(0, pipeline.get(1).cantidad());
        assertEquals(0, pipeline.get(2).cantidad());
    }

    @Test
    void resultadosComercialesUsanPaginacionDeVeinteYCierreReal() {
        authenticate("CRM_VIEW_ALL");
        CrmEtapaPipeline ganado = etapa(4L, "GANADO", 4, true, false);
        CrmEtapaPipeline perdido = etapa(5L, "PERDIDO", 5, false, true);
        CrmOportunidad ganada = oportunidad(21L, ganado, "GANADA");
        CrmOportunidad perdida = oportunidad(22L, perdido, "PERDIDA");
        when(oportunidadRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<CrmOportunidad>>any(),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(ganada, perdida)));

        var result = service.pageResultados(null, null, null, null, null, 0, 20);

        assertEquals(2, result.content().size());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(oportunidadRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<CrmOportunidad>>any(),
                pageableCaptor.capture()
        );
        assertEquals(20, pageableCaptor.getValue().getPageSize());
        assertEquals("fechaCierreReal", pageableCaptor.getValue().getSort().iterator().next().getProperty());
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

    private CrmEtapaPipeline etapa(Long id, String codigo, int orden, boolean ganado, boolean perdido) {
        CrmEtapaPipeline etapa = new CrmEtapaPipeline();
        etapa.setId(id);
        etapa.setCodigo(codigo);
        etapa.setNombre(codigo);
        etapa.setOrden(orden);
        etapa.setGanado(ganado);
        etapa.setPerdido(perdido);
        etapa.setActivo(true);
        return etapa;
    }

    private CrmOportunidad oportunidad(Long id, CrmEtapaPipeline etapa, String estado) {
        CrmOportunidad oportunidad = new CrmOportunidad();
        oportunidad.setId(id);
        oportunidad.setTitulo("Oportunidad " + id);
        oportunidad.setMontoEstimado(BigDecimal.valueOf(450));
        oportunidad.setProbabilidad(etapa.getProbabilidadDefault());
        oportunidad.setEtapaPipeline(etapa);
        oportunidad.setEtapa(etapa.getCodigo());
        oportunidad.setResponsableId("10");
        oportunidad.setEstado(estado);
        return oportunidad;
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario",
                "n/a",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        ));
    }
}
