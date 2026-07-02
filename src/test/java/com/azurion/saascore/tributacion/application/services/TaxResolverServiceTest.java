package com.azurion.saascore.tributacion.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.tributacion.application.dto.TaxResolution;
import com.azurion.saascore.tributacion.domain.entities.ConfiguracionTributariaEmpresa;
import com.azurion.saascore.tributacion.domain.repositories.ConfiguracionTributariaEmpresaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxResolverServiceTest {
    @Mock ProductoRepository productoRepository;
    @Mock SucursalRepository sucursalRepository;
    @Mock ConfiguracionTributariaEmpresaRepository configuracionRepository;

    private TaxResolverService service;
    private Producto producto;
    private Sucursal sucursal;
    private ConfiguracionTributariaEmpresa empresa;

    @BeforeEach
    void setUp() {
        service = new TaxResolverService(
                productoRepository,
                sucursalRepository,
                configuracionRepository,
                new TaxConfigurationValidator()
        );
        producto = new Producto();
        producto.setId(10L);
        producto.setUsaConfiguracionEmpresa(true);
        sucursal = new Sucursal();
        sucursal.setId(20L);
        empresa = new ConfiguracionTributariaEmpresa();
        empresa.setTipoOperacionDefaultId("0101");
        empresa.setTipoAfectacionDefaultId("10");
        empresa.setTributoDefaultId("1000");
        empresa.setPorcentajeIgvDefault(new BigDecimal("18.00"));
        empresa.setMonedaDefault("PEN");

        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(sucursalRepository.findById(20L)).thenReturn(Optional.of(sucursal));
        when(configuracionRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(empresa));
    }

    @Test
    void resolvesCompanyConfigurationByDefault() {
        TaxResolution result = service.resolverImpuesto(10L, 20L);

        assertThat(result.origen()).isEqualTo("EMPRESA");
        assertThat(result.tipoAfectacionCodigo()).isEqualTo("10");
        assertThat(result.tributoCodigo()).isEqualTo("1000");
        assertThat(result.porcentajeIgv()).isEqualByComparingTo("18.00");
    }

    @Test
    void branchOverridesCompanyConfiguration() {
        sucursal.setTipoAfectacionDefaultId("20");
        sucursal.setTributoDefaultId("9997");
        sucursal.setPorcentajeIgvDefault(BigDecimal.ZERO);

        TaxResolution result = service.resolverImpuesto(10L, 20L);

        assertThat(result.origen()).isEqualTo("SUCURSAL");
        assertThat(result.tipoAfectacionCodigo()).isEqualTo("20");
        assertThat(result.porcentajeIgv()).isZero();
    }

    @Test
    void legacyZeroPercentBranchOverridesCompanyAsExonerated() {
        sucursal.setIgvPorcentaje(BigDecimal.ZERO);

        TaxResolution result = service.resolverImpuesto(10L, 20L);

        assertThat(result.origen()).isEqualTo("SUCURSAL");
        assertThat(result.tipoAfectacionCodigo()).isEqualTo("20");
        assertThat(result.tributoCodigo()).isEqualTo("9997");
        assertThat(result.porcentajeIgv()).isZero();
    }

    @Test
    void productOverridesBranchAndCompanyConfiguration() {
        producto.setUsaConfiguracionEmpresa(false);
        producto.setTipoAfectacionIgvId("30");
        producto.setTributoId("9998");
        producto.setPorcentajeImpuesto(BigDecimal.ZERO);

        TaxResolution result = service.resolverImpuesto(10L, 20L);

        assertThat(result.origen()).isEqualTo("PRODUCTO");
        assertThat(result.tipoAfectacionCodigo()).isEqualTo("30");
        assertThat(result.porcentajeIgv()).isZero();
    }
}
