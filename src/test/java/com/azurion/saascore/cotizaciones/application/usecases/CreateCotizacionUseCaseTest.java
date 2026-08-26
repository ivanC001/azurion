package com.azurion.saascore.cotizaciones.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.clientes.domain.repositories.ClienteRepository;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionDetalleRequest;
import com.azurion.saascore.cotizaciones.application.dto.CotizacionResponse;
import com.azurion.saascore.cotizaciones.application.dto.CreateCotizacionRequest;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.repositories.CotizacionRepository;
import com.azurion.saascore.cotizaciones.domain.repositories.PromocionCotizacionRepository;
import com.azurion.saascore.cotizaciones.application.services.CommercialCurrencyService;
import com.azurion.saascore.cotizaciones.application.services.CommercialCurrencyService.CurrencySnapshot;
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCotizacionUseCaseTest {

    @Mock
    private CotizacionRepository cotizacionRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private SucursalRepository sucursalRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PromocionCotizacionRepository promocionRepository;
    @Mock
    private CrmCatalogoItemRepository catalogoItemRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private UsuarioTenantRepository usuarioTenantRepository;
    @Mock
    private CommercialCurrencyService commercialCurrencyService;

    private CreateCotizacionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCotizacionUseCase(
                cotizacionRepository,
                clienteRepository,
                sucursalRepository,
                productoRepository,
                promocionRepository,
                catalogoItemRepository,
                authorizationService,
                usuarioTenantRepository,
                commercialCurrencyService
        );
    }

    @Test
    void storesAnImmutableCatalogSnapshotInTheQuoteDetail() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(8L);
        sucursal.setCodigo("PRINCIPAL");
        sucursal.setNombre("Sucursal Principal");

        CrmCatalogoItem catalogItem = new CrmCatalogoItem();
        catalogItem.setId(24L);
        catalogItem.setTipoItem("CURSO");
        catalogItem.setNombre("Curso Java Spring Profesional");
        catalogItem.setDescripcion("Incluye arquitectura, seguridad y despliegue.");
        catalogItem.setMetadataJson("{\"atributos\":{\"duracion\":\"48 horas\",\"modalidad\":\"Virtual\"}}");
        catalogItem.setMoneda("USD");
        catalogItem.setPrecioReferencial(new BigDecimal("320.00"));
        catalogItem.setEstado("ACTIVO");

        UsuarioTenant advisor = new UsuarioTenant();
        advisor.setId(1L);
        advisor.setNombres("Maria");
        advisor.setApellidos("Lopez Ramirez");
        advisor.setTelefono("+51987654321");
        advisor.setEmail("maria.lopez@azurion.test");
        advisor.setCargo("Asesora comercial");
        advisor.setFotoPerfilUrl("/files/user-profiles/tenant-demo/user-1.jpg");

        when(authorizationService.currentUsuarioId()).thenReturn(1L);
        when(usuarioTenantRepository.findById(1L)).thenReturn(Optional.of(advisor));
        when(sucursalRepository.findById(8L)).thenReturn(Optional.of(sucursal));
        when(catalogoItemRepository.findById(24L)).thenReturn(Optional.of(catalogItem));
        CurrencySnapshot currencySnapshot = new CurrencySnapshot(
                "USD", "PEN", new BigDecimal("3.850000"), OffsetDateTime.parse("2026-08-09T10:00:00-05:00"));
        when(commercialCurrencyService.resolve("USD", "USD")).thenReturn(currencySnapshot);
        when(commercialCurrencyService.toBase(new BigDecimal("600.00"), currencySnapshot))
                .thenReturn(new BigDecimal("2310.00"));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(invocation -> {
            Cotizacion quote = invocation.getArgument(0);
            quote.setId(91L);
            return quote;
        });

        CreateCotizacionRequest request = new CreateCotizacionRequest(
                null,
                "seller-1",
                "Vendedor CRM",
                8L,
                LocalDate.of(2026, 8, 9),
                LocalDate.of(2026, 8, 16),
                "USD",
                "Propuesta comercial",
                100L,
                List.of(new CotizacionDetalleRequest(
                        null,
                        24L,
                        null,
                        null,
                        new BigDecimal("2"),
                        new BigDecimal("300.00"),
                        BigDecimal.ZERO
                ))
        );

        CotizacionResponse response = useCase.execute(request);

        assertThat(response.detalles()).hasSize(1);
        assertThat(response.detalles().getFirst().catalogoItemId()).isEqualTo(24L);
        assertThat(response.detalles().getFirst().catalogoNombre()).isEqualTo("Curso Java Spring Profesional");
        assertThat(response.detalles().getFirst().catalogoDescripcion())
                .isEqualTo("Incluye arquitectura, seguridad y despliegue.");
        assertThat(response.detalles().getFirst().catalogoMetadataJson()).contains("48 horas", "Virtual");
        assertThat(response.detalles().getFirst().catalogoMoneda()).isEqualTo("USD");
        assertThat(response.detalles().getFirst().catalogoPrecioReferencial())
                .isEqualByComparingTo("320.00");
        assertThat(response.total()).isEqualByComparingTo("600.00");
        assertThat(response.moneda()).isEqualTo("USD");
        assertThat(response.monedaBase()).isEqualTo("PEN");
        assertThat(response.tipoCambioAplicado()).isEqualByComparingTo("3.850000");
        assertThat(response.totalMonedaBase()).isEqualByComparingTo("2310.00");
        assertThat(response.usuarioId()).isEqualTo("1");
        assertThat(response.usuarioNombre()).isEqualTo("Maria");
        assertThat(response.asesorApellidos()).isEqualTo("Lopez Ramirez");
        assertThat(response.asesorTelefono()).isEqualTo("+51987654321");
        assertThat(response.asesorEmail()).isEqualTo("maria.lopez@azurion.test");
        assertThat(response.asesorCargo()).isEqualTo("Asesora comercial");
    }
}
