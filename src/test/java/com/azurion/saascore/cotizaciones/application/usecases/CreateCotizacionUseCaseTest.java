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
import com.azurion.saascore.crm.domain.entities.CrmCatalogoItem;
import com.azurion.saascore.crm.domain.repositories.CrmCatalogoItemRepository;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
                authorizationService
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

        when(authorizationService.currentUsuarioId()).thenReturn(1L);
        when(sucursalRepository.findById(8L)).thenReturn(Optional.of(sucursal));
        when(catalogoItemRepository.findById(24L)).thenReturn(Optional.of(catalogItem));
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
    }
}
