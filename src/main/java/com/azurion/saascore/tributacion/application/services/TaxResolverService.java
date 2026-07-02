package com.azurion.saascore.tributacion.application.services;

import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.tributacion.application.dto.TaxResolution;
import com.azurion.saascore.tributacion.domain.entities.ConfiguracionTributariaEmpresa;
import com.azurion.saascore.tributacion.domain.repositories.ConfiguracionTributariaEmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaxResolverService {
    private final ProductoRepository productoRepository;
    private final SucursalRepository sucursalRepository;
    private final ConfiguracionTributariaEmpresaRepository configuracionRepository;
    private final TaxConfigurationValidator validator;

    @Transactional(readOnly = true)
    public TaxResolution resolverImpuesto(Long productoId, Long sucursalId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado: " + productoId));
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada: " + sucursalId));
        ConfiguracionTributariaEmpresa empresa = configuracionRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException("CONFIGURACION_TRIBUTARIA_REQUERIDA", "Configura la tributacion de la empresa antes de vender"));
        return resolverImpuesto(producto, sucursal, empresa);
    }

    public TaxResolution resolverImpuesto(
            Producto producto,
            Sucursal sucursal,
            ConfiguracionTributariaEmpresa empresa
    ) {
        String tipoOperacion = firstNonBlank(sucursal.getTipoOperacionDefaultId(), empresa.getTipoOperacionDefaultId());
        String moneda = empresa.getMonedaDefault();

        if (!producto.isUsaConfiguracionEmpresa()) {
            validator.validateProducto(producto.isAfectoIgv(), producto.getTipoAfectacionIgvId(), producto.getTributoId(), producto.getPorcentajeImpuesto());
            return new TaxResolution(
                    tipoOperacion,
                    producto.getTipoAfectacionIgvId(),
                    producto.getTributoId(),
                    producto.getPorcentajeImpuesto(),
                    moneda,
                    "PRODUCTO"
            );
        }

        boolean legacySucursalOverride = hasLegacySucursalOverride(sucursal, empresa);
        String afectacion = legacySucursalOverride
                ? legacyAfectacion(sucursal.getIgvPorcentaje())
                : firstNonBlank(sucursal.getTipoAfectacionDefaultId(), empresa.getTipoAfectacionDefaultId());
        String tributo = legacySucursalOverride
                ? legacyTributo(sucursal.getIgvPorcentaje())
                : firstNonBlank(sucursal.getTributoDefaultId(), empresa.getTributoDefaultId());
        BigDecimal porcentaje = legacySucursalOverride
                ? sucursal.getIgvPorcentaje()
                : sucursal.getPorcentajeIgvDefault() == null
                        ? empresa.getPorcentajeIgvDefault()
                        : sucursal.getPorcentajeIgvDefault();
        validator.validate(afectacion, tributo, porcentaje, true);
        return new TaxResolution(tipoOperacion, afectacion, tributo, porcentaje, moneda,
                legacySucursalOverride || "SUCURSAL".equals(resolveOrigin(sucursal)) ? "SUCURSAL" : "EMPRESA");
    }

    @Transactional(readOnly = true)
    public ConfiguracionTributariaEmpresa configuracionEmpresa() {
        return configuracionRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException("CONFIGURACION_TRIBUTARIA_REQUERIDA", "Configura la tributacion de la empresa antes de vender"));
    }

    private String resolveOrigin(Sucursal sucursal) {
        return sucursal.getTipoAfectacionDefaultId() != null
                || sucursal.getTributoDefaultId() != null
                || sucursal.getPorcentajeIgvDefault() != null
                ? "SUCURSAL"
                : "EMPRESA";
    }

    private boolean hasLegacySucursalOverride(Sucursal sucursal, ConfiguracionTributariaEmpresa empresa) {
        return "EMPRESA".equals(resolveOrigin(sucursal))
                && sucursal.getIgvPorcentaje() != null
                && empresa.getPorcentajeIgvDefault() != null
                && sucursal.getIgvPorcentaje().compareTo(empresa.getPorcentajeIgvDefault()) != 0;
    }

    private String legacyAfectacion(BigDecimal porcentaje) {
        return porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "20" : "10";
    }

    private String legacyTributo(BigDecimal porcentaje) {
        return porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "9997" : "1000";
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
