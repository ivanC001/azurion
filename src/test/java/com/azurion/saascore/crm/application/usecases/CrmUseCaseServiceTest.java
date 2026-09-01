package com.azurion.saascore.crm.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.clientes.application.dto.ClienteResponse;
import com.azurion.saascore.clientes.application.usecases.CreateClienteUseCase;
import com.azurion.saascore.clientes.domain.entities.Cliente;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.cotizaciones.application.usecases.CreateCotizacionUseCase;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.crm.application.support.CrmAccessPolicy;
import com.azurion.saascore.crm.application.support.CrmCurrencyConverter;
import com.azurion.saascore.crm.application.dto.CreateCrmProspectoRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmOportunidadRequest;
import com.azurion.saascore.crm.application.dto.CreateCrmCatalogoItemRequest;
import com.azurion.saascore.crm.application.dto.CrmCatalogoItemResponse;
import com.azurion.saascore.crm.application.dto.RepartirCrmProspectosRequest;
import com.azurion.saascore.crm.application.dto.RepartirCrmProspectosResponse;
import com.azurion.saascore.crm.application.dto.PublicCrmLeadRequest;
import com.azurion.saascore.crm.application.dto.SendCrmOpportunityEmailRequest;
import com.azurion.saascore.crm.application.services.LandingLeadValidationService.LandingLeadContext;
import com.azurion.saascore.crm.application.services.LandingLeadValidationService;
import com.azurion.saascore.crm.application.services.CrmSecretEncryptionService;
import com.azurion.saascore.crm.application.services.CrmLeadAssignmentService;
import com.azurion.saascore.crm.application.services.CrmPhoneNormalizationService;
import com.azurion.saascore.crm.domain.entities.CrmEtapaPipeline;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.entities.CrmCanalTokenConfig;
import com.azurion.saascore.crm.domain.entities.CrmCurrencyConfig;
import com.azurion.saascore.crm.domain.entities.CrmActividad;
import com.azurion.saascore.crm.domain.entities.CrmOportunidad;
import com.azurion.saascore.crm.domain.entities.CrmProspecto;
import com.azurion.saascore.crm.domain.entities.CrmLandingConfig;
import com.azurion.saascore.crm.domain.entities.CrmPublicLeadSubmission;
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
import com.azurion.saascore.crm.domain.repositories.CrmPublicLeadSubmissionRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.settings.email.application.services.EmailSenderService;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.persistence.BusinessOperationLockService;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionDetalleRequest;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.crm.application.dto.GenerarCotizacionDesdeOportunidadRequest;
import java.time.OffsetDateTime;
import java.time.LocalDate;
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

    @Mock
    CrmLeadAssignmentService leadAssignmentService;

    @Mock
    CrmPublicLeadSubmissionRepository publicLeadSubmissionRepository;

    @Mock
    BusinessOperationLockService ingressLockService;

    @Mock
    CrmPhoneNormalizationService phoneNormalizationService;

    @Mock
    EmailSenderService emailSenderService;

    @Mock
    EmpresaRepository empresaRepository;

    @Mock
    UsuarioTenantRepository usuarioTenantRepository;

    CrmUseCaseService service;

    @BeforeEach
    void setUp() {
        // Los colaboradores extraidos se construyen de verdad sobre los mismos
        // mocks: asi estas pruebas siguen ejercitando el comportamiento real a
        // traves de la fachada, no una version simulada de el.
        CrmAccessPolicy accessPolicy = new CrmAccessPolicy(authorizationService);
        CrmCurrencyConverter currencyConverter = new CrmCurrencyConverter(
                currencyConfigRepository,
                empresaRepository
        );
        CrmConfiguracionUseCase configuracionUseCase = new CrmConfiguracionUseCase(
                currencyConfigRepository,
                canalTokenConfigRepository,
                oportunidadRepository,
                cotizacionRepository,
                crmSecretEncryptionService
        );
        CrmReporteUseCase reporteUseCase = new CrmReporteUseCase(
                prospectoRepository,
                oportunidadRepository,
                actividadRepository,
                etapaPipelineRepository,
                usuarioTenantRepository,
                accessPolicy,
                currencyConverter
        );

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
                landingLeadValidationService,
                prospectoInteresRepository,
                leadAssignmentService,
                publicLeadSubmissionRepository,
                ingressLockService,
                phoneNormalizationService,
                emailSenderService,
                empresaRepository,
                usuarioTenantRepository,
                configuracionUseCase,
                reporteUseCase,
                accessPolicy,
                currencyConverter
        );
        org.mockito.Mockito.lenient().when(phoneNormalizationService.resolveCountryCode(any()))
                .thenReturn("PE");
        org.mockito.Mockito.lenient().when(phoneNormalizationService.normalize(any(), any()))
                .thenAnswer(invocation -> {
                    String raw = invocation.getArgument(0);
                    String digits = raw == null ? null : raw.replaceAll("[^0-9]", "");
                    return new CrmPhoneNormalizationService.NormalizedPhone(
                            digits == null || digits.isBlank() ? null : digits,
                            digits == null || digits.isBlank() ? List.of() : List.of(digits)
                    );
                });
    }

    @Test
    void reporteResponsablesExponeNombreCompletoEnLugarDelId() {
        UsuarioTenant usuario = new UsuarioTenant();
        usuario.setId(4L);
        usuario.setUsername("vendedor1");
        usuario.setNombres("Rosa Maria");
        usuario.setApellidos("Perez Soto");
        when(usuarioTenantRepository.findAllByOrderByNombresAsc()).thenReturn(List.of(usuario));

        var responsables = service.reporteResponsables();

        assertEquals(1, responsables.size());
        assertEquals("4", responsables.getFirst().id());
        assertEquals("Rosa Maria Perez Soto", responsables.getFirst().nombre());
    }

    @Test
    void generarCotizacionActualizaValorEstimadoYMonedaDeLaOportunidad() {
        CrmOportunidad oportunidad = new CrmOportunidad();
        oportunidad.setId(31L);
        oportunidad.setResponsableId("7");
        oportunidad.setEstado("ABIERTA");
        oportunidad.setMoneda("PEN");
        oportunidad.setMontoEstimado(new BigDecimal("1100.00"));

        when(authorizationService.currentUsuarioId()).thenReturn(7L);
        when(oportunidadRepository.findById(31L)).thenReturn(Optional.of(oportunidad));
        when(empresaRepository.findByTenantId(any())).thenReturn(Optional.empty());
        when(createCotizacionUseCase.execute(any()))
                .thenReturn(cotizacionGenerada(new BigDecimal("152000.00")));
        when(oportunidadRepository.save(any(CrmOportunidad.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generarCotizacion(31L, new GenerarCotizacionDesdeOportunidadRequest(
                null,
                "7",
                "Vendedor Demo",
                5L,
                null,
                null,
                "PEN",
                null,
                List.of(new CotizacionDetalleRequest(null, null, null, "Toyota Hilux",
                        BigDecimal.ONE, new BigDecimal("152000.00"), BigDecimal.ZERO))
        ));

        assertEquals(new BigDecimal("152000.00"), oportunidad.getMontoEstimado());
        assertEquals("PEN", oportunidad.getMoneda());
    }

    private CotizacionResponse cotizacionGenerada(BigDecimal total) {
        return new CotizacionResponse(
                40L, null, null, null, "7", "Vendedor Demo",
                null, null, null, null, null,
                5L, "PRINCIPAL", "Sucursal Principal",
                LocalDate.now(), null,
                "PEN", "PEN", BigDecimal.ONE, OffsetDateTime.now(),
                total, total, total, total,
                "BORRADOR", null, null, 31L,
                null, null, null, null, null, null, null,
                null, null, List.of()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void catalogoSinMonedaUsaMonedaBaseDelTenant() {
        Empresa empresa = new Empresa();
        empresa.setMonedaCodigo("USD");
        when(empresaRepository.findByTenantId("public")).thenReturn(Optional.of(empresa));
        when(catalogoItemRepository.saveAndFlush(any(CrmCatalogoItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrmCatalogoItemResponse response = service.createCatalogoItem(new CreateCrmCatalogoItemRequest(
                "PRODUCTO",
                "Kit comercial",
                "Producto de prueba",
                BigDecimal.valueOf(125),
                null,
                "ACTIVO",
                null,
                true,
                null
        ));

        assertEquals("USD", response.moneda());
    }

    @Test
    void monedasPredeterminadasNoSePublicanHastaSerConfiguradas() {
        when(currencyConfigRepository.findAllByOrderByMonedaAsc()).thenReturn(List.of());

        var currencies = service.listCurrencyConfig();

        assertEquals(2, currencies.size());
        assertEquals(false, currencies.get(0).activo());
        assertEquals(false, currencies.get(1).activo());
    }

    @Test
    void catalogoAceptaMonedaExtranjeraConfiguradaYActiva() {
        Empresa empresa = new Empresa();
        empresa.setMonedaCodigo("PEN");
        CrmCurrencyConfig usd = new CrmCurrencyConfig();
        usd.setMoneda("USD");
        usd.setNombre("Dólar americano");
        usd.setSimbolo("$");
        usd.setTipoCambioBase(new BigDecimal("3.800000"));
        usd.setMargenConversionPorcentaje(BigDecimal.ZERO);
        usd.setActivo(true);

        when(empresaRepository.findByTenantId("public")).thenReturn(Optional.of(empresa));
        when(currencyConfigRepository.findByMoneda("USD")).thenReturn(Optional.of(usd));
        when(catalogoItemRepository.saveAndFlush(any(CrmCatalogoItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrmCatalogoItemResponse response = service.createCatalogoItem(new CreateCrmCatalogoItemRequest(
                "PRODUCTO",
                "Licencia internacional",
                "Producto cotizado en dólares",
                BigDecimal.valueOf(125),
                "USD",
                "ACTIVO",
                null,
                true,
                null
        ));

        assertEquals("USD", response.moneda());
    }

    @Test
    void catalogoRechazaMonedaExtranjeraInactiva() {
        Empresa empresa = new Empresa();
        empresa.setMonedaCodigo("PEN");
        CrmCurrencyConfig usd = new CrmCurrencyConfig();
        usd.setMoneda("USD");
        usd.setTipoCambioBase(new BigDecimal("3.800000"));
        usd.setActivo(false);

        when(empresaRepository.findByTenantId("public")).thenReturn(Optional.of(empresa));
        when(currencyConfigRepository.findByMoneda("USD")).thenReturn(Optional.of(usd));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.createCatalogoItem(new CreateCrmCatalogoItemRequest(
                        "PRODUCTO",
                        "Licencia internacional",
                        "Producto cotizado en dólares",
                        BigDecimal.valueOf(125),
                        "USD",
                        "ACTIVO",
                        null,
                        true,
                        null
                )));

        assertEquals("CRM_MONEDA_INACTIVA", exception.getCode());
        verify(catalogoItemRepository, never()).saveAndFlush(any(CrmCatalogoItem.class));
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
        assertEquals("PE", captor.getValue().getPaisCodigo());
    }

    @Test
    void retryConMismaIdempotencyKeyDevuelveElMismoComprobante() {
        CrmPublicLeadSubmission previous = new CrmPublicLeadSubmission();
        previous.setReceiptId("LD-EXISTENTE");
        previous.setEstado("PROCESSED");
        previous.setReceivedAt(OffsetDateTime.parse("2026-07-25T10:00:00-05:00"));
        when(publicLeadSubmissionRepository.findByIdempotencyHash(anyString()))
                .thenReturn(Optional.of(previous));

        var response = service.capturePublicLead(publicLeadRequest(), "BROWSER", "submission-123");

        assertEquals("LD-EXISTENTE", response.receiptId());
        verify(ingressLockService).lockAll(any());
        verify(landingLeadValidationService, never()).validate(any());
    }

    @Test
    void rechazaCuandoTelefonoYCorreoYaPertenecenAProspectosDiferentes() {
        CrmLandingConfig landing = new CrmLandingConfig();
        landing.setValidarDuplicadosPor("TELEFONO_CORREO");
        when(landingLeadValidationService.validate(any())).thenReturn(
                new LandingLeadContext(landing, null, true, true, "LANDING", "municipios", null)
        );
        when(phoneNormalizationService.resolveCountryCode(any())).thenReturn("PE");
        when(phoneNormalizationService.normalize(any(), any())).thenReturn(
                new CrmPhoneNormalizationService.NormalizedPhone(
                        "51999999999",
                        List.of("51999999999", "999999999")
                )
        );
        CrmProspecto phoneOwner = new CrmProspecto();
        phoneOwner.setId(10L);
        CrmProspecto emailOwner = new CrmProspecto();
        emailOwner.setId(20L);
        when(prospectoRepository.findFirstByTelefonoNormalizado("51999999999"))
                .thenReturn(Optional.of(phoneOwner));
        when(prospectoRepository.findFirstByCorreoIgnoreCaseOrderByIdDesc("juan@perez.com"))
                .thenReturn(Optional.of(emailOwner));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.capturePublicLead(publicLeadRequest(), "BROWSER", null)
        );

        assertEquals("CRM_LEAD_IDENTIDAD_CONFLICTO", exception.getCode());
        verify(prospectoRepository, never()).save(any());
    }

    @Test
    void leadPublicoSinTipoNiDocumentoQuedaPorClasificar() {
        preparePublicLeadCapture();

        service.capturePublicLead(publicLeadRequest(null, null, null), "BROWSER", null);

        ArgumentCaptor<CrmProspecto> captor = ArgumentCaptor.forClass(CrmProspecto.class);
        verify(prospectoRepository).save(captor.capture());
        assertEquals("SIN_DEFINIR", captor.getValue().getTipoPersona());
    }

    @Test
    void leadPublicoConRucSeClasificaComoEmpresa() {
        preparePublicLeadCapture();

        service.capturePublicLead(publicLeadRequest(null, "RUC", "20123456789"), "SERVER", null);

        ArgumentCaptor<CrmProspecto> captor = ArgumentCaptor.forClass(CrmProspecto.class);
        verify(prospectoRepository).save(captor.capture());
        assertEquals("JURIDICA", captor.getValue().getTipoPersona());
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
    void administradorEliminaLeadNuevoSinHistorialComercial() {
        authenticate("CRM_DELETE");
        CrmProspecto prospecto = prospecto(21L);
        when(prospectoRepository.findById(21L)).thenReturn(Optional.of(prospecto));
        when(oportunidadRepository.existsByProspecto_Id(21L)).thenReturn(false);

        service.deleteProspecto(21L);

        verify(actividadRepository).deleteByProspecto_Id(21L);
        verify(prospectoRepository).delete(prospecto);
    }

    @Test
    void noEliminaProspectoQueYaTieneOportunidad() {
        authenticate("CRM_DELETE");
        CrmProspecto prospecto = prospecto(22L);
        prospecto.setOportunidadId(90L);
        when(prospectoRepository.findById(22L)).thenReturn(Optional.of(prospecto));

        BusinessException error = assertThrows(BusinessException.class, () -> service.deleteProspecto(22L));

        assertEquals("CRM_PROSPECTO_CON_HISTORIAL", error.getCode());
        verify(prospectoRepository, never()).delete(prospecto);
    }

    @Test
    void prospectoSinClasificarNoPuedeConvertirseACliente() {
        authenticate("CRM_VIEW_ALL");
        CrmProspecto prospecto = new CrmProspecto();
        prospecto.setTipoPersona("SIN_DEFINIR");
        prospecto.setNombre("Contacto por clasificar");
        prospecto.setResponsableId("10");
        when(prospectoRepository.findById(17L)).thenReturn(Optional.of(prospecto));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.convertirProspectoCliente(17L)
        );

        assertEquals("CRM_TIPO_CLIENTE_REQUERIDO", exception.getCode());
        verify(createClienteUseCase, never()).execute(any());
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
        when(etapaPipelineRepository.findByActivoTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(interesado, cotizado, negociacion, ganado, perdido));
        when(oportunidadRepository.summarizeBoardByStage("ABIERTA", null, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(stageTotals(1L, 1L, new BigDecimal("450.00"))));
        when(oportunidadRepository.findBoardCardsByStage(
                org.mockito.ArgumentMatchers.eq("ABIERTA"),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.isNull(),
                any()))
                .thenReturn(List.of(abierta));

        var pipeline = service.pipeline();

        assertEquals(List.of("INTERESADO", "COTIZADO", "NEGOCIACION"),
                pipeline.stream().map(column -> column.etapa().codigo()).toList());
        assertEquals(1, pipeline.getFirst().cantidad());
        assertEquals(new BigDecimal("450.00"), pipeline.getFirst().monto());
        assertEquals(0, pipeline.get(1).cantidad());
        assertEquals(0, pipeline.get(2).cantidad());
    }

    @Test
    void pipelineNoCargaTodasLasOportunidadesDelTenant() {
        authenticate("CRM_VIEW_ALL");
        CrmEtapaPipeline interesado = etapa(1L, "INTERESADO", 1, false, false);
        when(etapaPipelineRepository.findByActivoTrueOrderByOrdenAscIdAsc())
                .thenReturn(List.of(interesado));
        when(oportunidadRepository.summarizeBoardByStage("ABIERTA", null, List.of(1L)))
                .thenReturn(List.of(stageTotals(1L, 0L, BigDecimal.ZERO)));

        service.pipeline();

        // El coste del tablero debe depender de lo que muestra, no del historico.
        verify(oportunidadRepository, never()).findAllByOrderByIdDesc();
        // Una columna vacia no necesita pedir tarjetas.
        verify(oportunidadRepository, never()).findBoardCardsByStage(
                anyString(), org.mockito.ArgumentMatchers.anyLong(), any(), any());
    }

    private CrmOportunidadRepository.StageBoardTotalsProjection stageTotals(Long etapaId, long cantidad, BigDecimal monto) {
        return new CrmOportunidadRepository.StageBoardTotalsProjection() {
            @Override
            public Long getEtapaId() {
                return etapaId;
            }

            @Override
            public long getCantidad() {
                return cantidad;
            }

            @Override
            public BigDecimal getMonto() {
                return monto;
            }
        };
    }

    @Test
    void oportunidadRechazaMontoCeroAunqueLaPeticionOmitaLaValidacionHttp() {
        authenticate("CRM_VIEW_ALL");
        when(authorizationService.currentUsuarioId()).thenReturn(10L);
        Cliente cliente = new Cliente();
        cliente.setId(8L);
        cliente.setNombre("Cliente pipeline");
        CrmCatalogoItem catalogo = new CrmCatalogoItem();
        catalogo.setId(5L);
        catalogo.setTipoItem("SERVICIO");
        catalogo.setNombre("Implementacion CRM");
        catalogo.setPrecioReferencial(BigDecimal.valueOf(450));
        catalogo.setEstado("ACTIVO");
        CrmEtapaPipeline interesado = etapa(1L, "INTERESADO", 1, false, false);
        interesado.setProbabilidadDefault(30);
        when(clienteRepository.findById(8L)).thenReturn(Optional.of(cliente));
        when(catalogoItemRepository.findById(5L)).thenReturn(Optional.of(catalogo));
        when(etapaPipelineRepository.findByCodigo("INTERESADO")).thenReturn(Optional.of(interesado));

        BusinessException error = assertThrows(BusinessException.class, () -> service.createOportunidad(
                new CreateCrmOportunidadRequest(
                        null,
                        8L,
                        "SERVICIO",
                        5L,
                        "Implementacion CRM",
                        null,
                        BigDecimal.ZERO,
                        30,
                        "INTERESADO",
                        LocalDate.now().plusDays(15),
                        "10",
                        "Llamada inicial",
                        OffsetDateTime.now().plusDays(1)
                )
        ));

        assertEquals("CRM_OPORTUNIDAD_MONTO_REQUERIDO", error.getCode());
        verify(oportunidadRepository, never()).save(any(CrmOportunidad.class));
    }

    @Test
    void oportunidadNuevaCreaLaSiguienteAccionObligatoria() {
        authenticate("CRM_VIEW_ALL");
        when(authorizationService.currentUsuarioId()).thenReturn(10L);
        Cliente cliente = new Cliente();
        cliente.setId(8L);
        cliente.setNombre("Cliente pipeline");
        CrmCatalogoItem catalogo = new CrmCatalogoItem();
        catalogo.setId(5L);
        catalogo.setTipoItem("SERVICIO");
        catalogo.setNombre("Implementacion CRM");
        catalogo.setPrecioReferencial(BigDecimal.valueOf(450));
        catalogo.setMoneda("USD");
        catalogo.setEstado("ACTIVO");
        CrmEtapaPipeline interesado = etapa(1L, "INTERESADO", 1, false, false);
        interesado.setProbabilidadDefault(30);
        when(clienteRepository.findById(8L)).thenReturn(Optional.of(cliente));
        when(catalogoItemRepository.findById(5L)).thenReturn(Optional.of(catalogo));
        when(etapaPipelineRepository.findByCodigo("INTERESADO")).thenReturn(Optional.of(interesado));
        when(oportunidadRepository.save(any(CrmOportunidad.class))).thenAnswer(invocation -> {
            CrmOportunidad saved = invocation.getArgument(0);
            saved.setId(71L);
            return saved;
        });

        var response = service.createOportunidad(new CreateCrmOportunidadRequest(
                null,
                8L,
                "SERVICIO",
                5L,
                "Implementacion CRM",
                null,
                BigDecimal.valueOf(450),
                30,
                "INTERESADO",
                LocalDate.now().plusDays(15),
                "10",
                "Llamada inicial",
                OffsetDateTime.now().plusDays(1)
        ));

        ArgumentCaptor<CrmActividad> activityCaptor = ArgumentCaptor.forClass(CrmActividad.class);
        verify(actividadRepository).save(activityCaptor.capture());
        assertEquals("Llamada inicial", activityCaptor.getValue().getAsunto());
        assertEquals("PENDIENTE", activityCaptor.getValue().getEstado());
        assertEquals(71L, activityCaptor.getValue().getOportunidad().getId());
        verify(oportunidadRepository).save(argThat(saved -> "USD".equals(saved.getMoneda())));
        assertEquals("USD", response.moneda());
    }

    @Test
    void correoDeOportunidadUsaSmtpDelTenantYRegistraActividad() {
        authenticate("CRM_VIEW_ALL");
        when(authorizationService.currentUsuarioId()).thenReturn(10L);
        CrmEtapaPipeline interesado = etapa(1L, "INTERESADO", 1, false, false);
        CrmOportunidad oportunidad = oportunidad(71L, interesado, "ABIERTA");
        Cliente cliente = new Cliente();
        cliente.setId(8L);
        cliente.setEmail("cliente@empresa.com");
        oportunidad.setCliente(cliente);
        when(oportunidadRepository.findById(71L)).thenReturn(Optional.of(oportunidad));

        var response = service.sendOpportunityEmail(
                71L,
                new SendCrmOpportunityEmailRequest("Propuesta comercial", "Hola, adjunto el seguimiento.")
        );

        assertEquals("cliente@empresa.com", response.destinatario());
        verify(emailSenderService).sendEmail(
                "public",
                "cliente@empresa.com",
                "Propuesta comercial",
                "Hola, adjunto el seguimiento.",
                List.of()
        );
        ArgumentCaptor<CrmActividad> activityCaptor = ArgumentCaptor.forClass(CrmActividad.class);
        verify(actividadRepository).save(activityCaptor.capture());
        assertEquals("CORREO", activityCaptor.getValue().getTipoActividad());
        assertEquals("REALIZADA", activityCaptor.getValue().getEstado());
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
                "PE",
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

    private PublicCrmLeadRequest publicLeadRequest() {
        return publicLeadRequest("NATURAL", null, null);
    }

    private PublicCrmLeadRequest publicLeadRequest(
            String tipoPersona,
            String tipoDocumento,
            String numeroDocumento) {
        return new PublicCrmLeadRequest(
                "empresa_demo",
                "lnd_publica",
                tipoPersona,
                tipoDocumento,
                numeroDocumento,
                "Juan Perez",
                null,
                "juan@perez.com",
                "+51 999 999 999",
                null,
                null,
                "WEB",
                "LANDING",
                "municipios",
                "https://landing.example/contacto",
                "Deseo informacion",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                null
        );
    }

    private void preparePublicLeadCapture() {
        CrmLandingConfig landing = new CrmLandingConfig();
        landing.setValidarDuplicadosPor("NINGUNO");
        when(landingLeadValidationService.validate(any())).thenReturn(
                new LandingLeadContext(landing, null, false, true, "LANDING", "municipios", null)
        );
        when(phoneNormalizationService.resolveCountryCode(any())).thenReturn("PE");
        when(phoneNormalizationService.normalize(any(), any())).thenReturn(
                new CrmPhoneNormalizationService.NormalizedPhone(null, List.of())
        );
        when(prospectoRepository.save(any(CrmProspecto.class))).thenAnswer(invocation -> {
            CrmProspecto prospecto = invocation.getArgument(0);
            prospecto.setId(81L);
            return prospecto;
        });
        when(publicLeadSubmissionRepository.save(any(CrmPublicLeadSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "usuario",
                "n/a",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        ));
    }
}
