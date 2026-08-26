package com.azurion.saascore.cotizaciones.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.azurion.saascore.cotizaciones.application.dto.CotizacionPdfResponse;
import com.azurion.saascore.cotizaciones.application.services.CotizacionClienteData;
import com.azurion.saascore.cotizaciones.application.services.CotizacionClienteDataResolver;
import com.azurion.saascore.cotizaciones.domain.entities.Cotizacion;
import com.azurion.saascore.cotizaciones.domain.entities.CotizacionDetalle;
import com.azurion.saascore.empresas.application.usecases.GetCurrentEmpresaUseCase;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateCotizacionPdfUseCaseTest {

    @Mock
    private GetCotizacionUseCase getCotizacionUseCase;
    @Mock
    private GetCurrentEmpresaUseCase getCurrentEmpresaUseCase;
    @Mock
    private CotizacionClienteDataResolver clienteDataResolver;

    private GenerateCotizacionPdfUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GenerateCotizacionPdfUseCase(
                getCotizacionUseCase,
                getCurrentEmpresaUseCase,
                clienteDataResolver,
                new ObjectMapper()
        );
    }

    @Test
    void addsTheCatalogCommercialSheetToTheGeneratedPdf() throws Exception {
        Cotizacion quote = quoteWithCatalogSnapshot();
        Empresa empresa = new Empresa();
        empresa.setRazonSocial("AZURION DEMO SAC");
        empresa.setRuc("20000000001");

        when(getCotizacionUseCase.find(91L)).thenReturn(quote);
        when(getCurrentEmpresaUseCase.resolveCurrentEmpresa()).thenReturn(empresa);
        when(clienteDataResolver.resolveForEmission(quote)).thenReturn(new CotizacionClienteData(
                "Empresa Cliente SAC",
                "RUC",
                "20123456789",
                "compras@cliente.test",
                "+51999999999",
                "Av. Principal 123"
        ));

        CotizacionPdfResponse response = useCase.execute(91L);
        byte[] pdfBytes = Base64.getDecoder().decode(response.base64());

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            assertThat(text).contains(
                    "FICHA COMERCIAL DEL PRODUCTO",
                    "Curso Java Spring Profesional",
                    "Maria Lopez Ramirez",
                    "Asesora comercial",
                    "+51 987 654 321",
                    "maria.lopez@azurion.test",
                    "DURACION",
                    "48 horas",
                    "MODALIDAD",
                    "Virtual"
            );
        }
    }

    private Cotizacion quoteWithCatalogSnapshot() {
        Sucursal sucursal = new Sucursal();
        sucursal.setId(8L);
        sucursal.setCodigo("PRINCIPAL");
        sucursal.setNombre("Sucursal Principal");
        sucursal.setDireccion("Av. Demo 456");

        Cotizacion quote = new Cotizacion();
        quote.setId(91L);
        quote.setSucursal(sucursal);
        quote.setUsuarioId("seller-1");
        quote.setUsuarioNombre("Maria");
        quote.setAsesorApellidos("Lopez Ramirez");
        quote.setAsesorTelefono("+51 987 654 321");
        quote.setAsesorEmail("maria.lopez@azurion.test");
        quote.setAsesorCargo("Asesora comercial");
        quote.setFechaEmision(LocalDate.of(2026, 8, 9));
        quote.setFechaVencimiento(LocalDate.of(2026, 8, 16));
        quote.setMoneda("USD");
        quote.setSubtotal(new BigDecimal("600.00"));
        quote.setTotal(new BigDecimal("600.00"));
        quote.setEstado("BORRADOR");
        quote.setObservacion("Propuesta comercial completa.");

        CotizacionDetalle detail = new CotizacionDetalle();
        detail.setCotizacion(quote);
        detail.setCatalogoItemId(24L);
        detail.setCatalogoTipoItem("CURSO");
        detail.setCatalogoNombre("Curso Java Spring Profesional");
        detail.setCatalogoDescripcion("Incluye arquitectura, seguridad y despliegue.");
        detail.setCatalogoMetadataJson("{\"atributos\":{\"duracion\":\"48 horas\",\"modalidad\":\"Virtual\"}}");
        detail.setCatalogoMoneda("USD");
        detail.setCatalogoPrecioReferencial(new BigDecimal("320.00"));
        detail.setDescripcion("Curso Java Spring Profesional");
        detail.setCantidad(new BigDecimal("2"));
        detail.setPrecioUnitario(new BigDecimal("300.00"));
        detail.setDescuento(BigDecimal.ZERO);
        detail.setPromocionDescuento(BigDecimal.ZERO);
        detail.setTotal(new BigDecimal("600.00"));
        quote.getDetalles().add(detail);
        return quote;
    }
}
