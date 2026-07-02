package com.azurion.saascore.tributacion.application.services;

import com.azurion.saascore.inventory.domain.entities.Producto;
import com.azurion.saascore.inventory.domain.repositories.ProductoRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.tributacion.application.dto.ConfiguracionTributariaRequest;
import com.azurion.saascore.tributacion.application.dto.ConfiguracionTributariaResponse;
import com.azurion.saascore.tributacion.application.dto.ProductoTributariaRequest;
import com.azurion.saascore.tributacion.application.dto.SucursalTributariaRequest;
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
public class ConfiguracionTributariaService {
    private final ConfiguracionTributariaEmpresaRepository configuracionRepository;
    private final SucursalRepository sucursalRepository;
    private final ProductoRepository productoRepository;
    private final TaxResolverService taxResolverService;
    private final TaxConfigurationValidator validator;

    @Transactional(readOnly = true)
    public ConfiguracionTributariaResponse getEmpresa() {
        return toResponse(requireEmpresa());
    }

    @Transactional
    public ConfiguracionTributariaResponse updateEmpresa(ConfiguracionTributariaRequest request) {
        validator.validate(request.tipoAfectacionDefaultId(), request.tributoDefaultId(), request.porcentajeIgvDefault(), true);
        ConfiguracionTributariaEmpresa entity = requireEmpresa();
        entity.setTipoOperacionDefaultId(request.tipoOperacionDefaultId().trim());
        entity.setTipoAfectacionDefaultId(request.tipoAfectacionDefaultId().trim());
        entity.setTributoDefaultId(request.tributoDefaultId().trim());
        entity.setPorcentajeIgvDefault(request.porcentajeIgvDefault());
        entity.setMonedaDefault(request.monedaDefault().trim().toUpperCase());
        entity.setEstado(blankToDefault(request.estado(), "ACTIVO").toUpperCase());
        return toResponse(configuracionRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public TaxResolution getSucursal(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        return resolveSucursalConfiguration(sucursal);
    }

    @Transactional
    public TaxResolution updateSucursal(Long sucursalId, SucursalTributariaRequest request) {
        validator.validate(request.tipoAfectacionDefaultId(), request.tributoDefaultId(), request.porcentajeIgvDefault(), false);
        Sucursal sucursal = sucursalRepository.findById(sucursalId)
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        sucursal.setTipoOperacionDefaultId(trim(request.tipoOperacionDefaultId()));
        sucursal.setTipoAfectacionDefaultId(trim(request.tipoAfectacionDefaultId()));
        sucursal.setTributoDefaultId(trim(request.tributoDefaultId()));
        sucursal.setPorcentajeIgvDefault(request.porcentajeIgvDefault());
        sucursal.setIgvPorcentaje(request.porcentajeIgvDefault() == null
                ? requireEmpresa().getPorcentajeIgvDefault()
                : request.porcentajeIgvDefault());
        sucursalRepository.save(sucursal);
        return resolveSucursalConfiguration(sucursal);
    }

    @Transactional(readOnly = true)
    public TaxResolution getProducto(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));
        return taxResolverService.resolverImpuesto(productoId, producto.getAlmacen().getSucursal().getId());
    }

    @Transactional
    public TaxResolution updateProducto(Long productoId, ProductoTributariaRequest request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException("PRODUCTO_NO_ENCONTRADO", "Producto no encontrado"));
        if (!request.usaConfiguracionEmpresa()) {
            validator.validateProducto(request.afectoIgv() == null || request.afectoIgv(), request.tipoAfectacionIgvId(), request.tributoId(), request.porcentajeImpuesto());
        }
        producto.setUsaConfiguracionEmpresa(request.usaConfiguracionEmpresa());
        producto.setAfectoIgv(request.afectoIgv() == null || request.afectoIgv());
        producto.setTipoAfectacionIgvId(trim(request.tipoAfectacionIgvId()));
        producto.setTributoId(trim(request.tributoId()));
        producto.setPorcentajeImpuesto(request.porcentajeImpuesto());
        productoRepository.save(producto);
        return taxResolverService.resolverImpuesto(productoId, producto.getAlmacen().getSucursal().getId());
    }

    private TaxResolution resolveSucursalConfiguration(Sucursal sucursal) {
        ConfiguracionTributariaEmpresa empresa = requireEmpresa();
        boolean legacyOverride = sucursal.getTipoAfectacionDefaultId() == null
                && sucursal.getTributoDefaultId() == null
                && sucursal.getPorcentajeIgvDefault() == null
                && sucursal.getIgvPorcentaje() != null
                && empresa.getPorcentajeIgvDefault() != null
                && sucursal.getIgvPorcentaje().compareTo(empresa.getPorcentajeIgvDefault()) != 0;
        return new TaxResolution(
                firstNonBlank(sucursal.getTipoOperacionDefaultId(), empresa.getTipoOperacionDefaultId()),
                legacyOverride ? legacyAfectacion(sucursal.getIgvPorcentaje()) : firstNonBlank(sucursal.getTipoAfectacionDefaultId(), empresa.getTipoAfectacionDefaultId()),
                legacyOverride ? legacyTributo(sucursal.getIgvPorcentaje()) : firstNonBlank(sucursal.getTributoDefaultId(), empresa.getTributoDefaultId()),
                legacyOverride ? sucursal.getIgvPorcentaje() : sucursal.getPorcentajeIgvDefault() == null ? empresa.getPorcentajeIgvDefault() : sucursal.getPorcentajeIgvDefault(),
                empresa.getMonedaDefault(),
                !legacyOverride && sucursal.getTipoAfectacionDefaultId() == null && sucursal.getTributoDefaultId() == null && sucursal.getPorcentajeIgvDefault() == null
                        ? "EMPRESA"
                        : "SUCURSAL"
        );
    }

    private String legacyAfectacion(BigDecimal porcentaje) {
        return porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "20" : "10";
    }

    private String legacyTributo(BigDecimal porcentaje) {
        return porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "9997" : "1000";
    }

    private ConfiguracionTributariaEmpresa requireEmpresa() {
        return configuracionRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new BusinessException("CONFIGURACION_TRIBUTARIA_REQUERIDA", "No existe configuracion tributaria de empresa"));
    }

    private ConfiguracionTributariaResponse toResponse(ConfiguracionTributariaEmpresa value) {
        return new ConfiguracionTributariaResponse(
                value.getId(), value.getTipoOperacionDefaultId(), value.getTipoAfectacionDefaultId(),
                value.getTributoDefaultId(), value.getPorcentajeIgvDefault(), value.getMonedaDefault(), value.getEstado()
        );
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
