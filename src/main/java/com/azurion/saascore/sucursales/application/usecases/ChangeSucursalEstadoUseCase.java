package com.azurion.saascore.sucursales.application.usecases;

import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangeSucursalEstadoUseCase {

    private final SucursalRepository sucursalRepository;
    private final SucursalOperationalGuard operationalGuard;

    @Transactional
    public SucursalResponse execute(Long id, boolean activo) {
        Sucursal sucursal = sucursalRepository.findById(id)
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        if (!activo) {
            operationalGuard.validateCanDeactivate(sucursal);
        }
        sucursal.setActivo(activo);
        Sucursal saved = sucursalRepository.save(sucursal);
        return new SucursalResponse(
                saved.getId(), saved.getCodigo(), saved.getNombre(), saved.getDireccion(),
                saved.getUbigeoCodigo(), saved.getDepartamento(), saved.getProvincia(),
                saved.getDistrito(), saved.getIgvPorcentaje(), saved.getTipoOperacionDefaultId(),
                saved.getTipoAfectacionDefaultId(), saved.getTributoDefaultId(), saved.getPorcentajeIgvDefault(),
                saved.isActivo()
        );
    }
}
