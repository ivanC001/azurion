package com.azurion.saascore.sucursales.application.usecases;

import com.azurion.saascore.sucursales.application.dto.CreateSucursalRequest;
import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.ubigeos.domain.entities.Ubigeo;
import com.azurion.saascore.ubigeos.domain.repositories.UbigeoRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateSucursalUseCase {

    private final SucursalRepository sucursalRepository;
    private final UbigeoRepository ubigeoRepository;

    public SucursalResponse execute(CreateSucursalRequest request) {
        String codigo = request.codigo().trim().toUpperCase();
        if (sucursalRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("SUCURSAL_DUPLICADA", "Ya existe una sucursal con ese codigo");
        }

        String ubigeoCodigo = sanitizeUbigeo(request.ubigeoCodigo());
        Ubigeo ubigeo = ubigeoRepository.findByCodigo(ubigeoCodigo)
                .orElseThrow(() -> new BusinessException("UBIGEO_NO_ENCONTRADO", "No existe ubigeo SUNAT: " + ubigeoCodigo));

        BigDecimal igvPorcentaje = request.igvPorcentaje();
        if (igvPorcentaje.compareTo(BigDecimal.ZERO) < 0 || igvPorcentaje.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessException("IGV_INVALIDO", "El IGV debe estar entre 0 y 100");
        }

        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo(codigo);
        sucursal.setNombre(request.nombre().trim());
        sucursal.setDireccion(trim(request.direccion()));
        sucursal.setUbigeoCodigo(ubigeo.getCodigo());
        sucursal.setDepartamento(ubigeo.getDepartamento());
        sucursal.setProvincia(ubigeo.getProvincia());
        sucursal.setDistrito(ubigeo.getDistrito());
        sucursal.setIgvPorcentaje(igvPorcentaje);
        applyTaxConfiguration(sucursal, igvPorcentaje);
        sucursal.setActivo(true);

        Sucursal saved = sucursalRepository.save(sucursal);
        return toResponse(saved);
    }

    private SucursalResponse toResponse(Sucursal saved) {
        return new SucursalResponse(
                saved.getId(),
                saved.getCodigo(),
                saved.getNombre(),
                saved.getDireccion(),
                saved.getUbigeoCodigo(),
                saved.getDepartamento(),
                saved.getProvincia(),
                saved.getDistrito(),
                saved.getIgvPorcentaje(),
                saved.getTipoOperacionDefaultId(),
                saved.getTipoAfectacionDefaultId(),
                saved.getTributoDefaultId(),
                saved.getPorcentajeIgvDefault(),
                saved.isActivo()
        );
    }

    private String sanitizeUbigeo(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D+", "");
        if (digits.length() < 6) {
            throw new BusinessException("UBIGEO_INVALIDO", "El ubigeo debe tener 6 digitos");
        }
        return digits.substring(0, 6);
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void applyTaxConfiguration(Sucursal sucursal, BigDecimal porcentaje) {
        sucursal.setTipoOperacionDefaultId("0101");
        sucursal.setTipoAfectacionDefaultId(porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "20" : "10");
        sucursal.setTributoDefaultId(porcentaje.compareTo(BigDecimal.ZERO) == 0 ? "9997" : "1000");
        sucursal.setPorcentajeIgvDefault(porcentaje);
    }
}
