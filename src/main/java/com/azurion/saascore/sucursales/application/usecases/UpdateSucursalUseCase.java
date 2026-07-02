package com.azurion.saascore.sucursales.application.usecases;

import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.application.dto.UpdateSucursalRequest;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.ubigeos.domain.entities.Ubigeo;
import com.azurion.saascore.ubigeos.domain.repositories.UbigeoRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateSucursalUseCase {

    private final SucursalRepository sucursalRepository;
    private final UbigeoRepository ubigeoRepository;

    @Transactional
    public SucursalResponse execute(Long id, UpdateSucursalRequest request) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        String codigo = request.codigo().trim().toUpperCase();
        if (sucursalRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new BusinessException("SUCURSAL_DUPLICADA", "Ya existe una sucursal con ese codigo");
        }

        String ubigeoCodigo = sanitizeUbigeo(request.ubigeoCodigo());
        Ubigeo ubigeo = ubigeoRepository.findByCodigo(ubigeoCodigo)
                .orElseThrow(() -> new BusinessException("UBIGEO_NO_ENCONTRADO", "No existe ubigeo SUNAT: " + ubigeoCodigo));
        validateIgv(request.igvPorcentaje());

        sucursal.setCodigo(codigo);
        sucursal.setNombre(request.nombre().trim());
        sucursal.setDireccion(trim(request.direccion()));
        sucursal.setUbigeoCodigo(ubigeo.getCodigo());
        sucursal.setDepartamento(ubigeo.getDepartamento());
        sucursal.setProvincia(ubigeo.getProvincia());
        sucursal.setDistrito(ubigeo.getDistrito());
        sucursal.setIgvPorcentaje(request.igvPorcentaje());
        if (sucursal.getTipoAfectacionDefaultId() == null
                && sucursal.getTributoDefaultId() == null
                && sucursal.getPorcentajeIgvDefault() == null) {
            applyTaxConfiguration(sucursal, request.igvPorcentaje());
        }
        return toResponse(sucursalRepository.save(sucursal));
    }

    private void validateIgv(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessException("IGV_INVALIDO", "El IGV debe estar entre 0 y 100");
        }
    }

    private String sanitizeUbigeo(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D+", "");
        if (digits.length() < 6) {
            throw new BusinessException("UBIGEO_INVALIDO", "El ubigeo debe tener 6 digitos");
        }
        return digits.substring(0, 6);
    }

    private String trim(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private SucursalResponse toResponse(Sucursal value) {
        return new SucursalResponse(
                value.getId(), value.getCodigo(), value.getNombre(), value.getDireccion(),
                value.getUbigeoCodigo(), value.getDepartamento(), value.getProvincia(),
                value.getDistrito(), value.getIgvPorcentaje(), value.getTipoOperacionDefaultId(),
                value.getTipoAfectacionDefaultId(), value.getTributoDefaultId(), value.getPorcentajeIgvDefault(), value.isActivo()
        );
    }

    private void applyTaxConfiguration(Sucursal sucursal, BigDecimal porcentaje) {
        sucursal.setTipoOperacionDefaultId("0101");
        sucursal.setTipoAfectacionDefaultId(porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "20" : "10");
        sucursal.setTributoDefaultId(porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "9997" : "1000");
        sucursal.setPorcentajeIgvDefault(porcentaje);
    }
}
