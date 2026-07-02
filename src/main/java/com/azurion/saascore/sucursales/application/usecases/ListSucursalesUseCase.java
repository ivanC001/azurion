package com.azurion.saascore.sucursales.application.usecases;

import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSucursalesUseCase {

    private final SucursalRepository sucursalRepository;

    public List<SucursalResponse> execute() {
        return sucursalRepository.findAllByOrderByNombreAsc().stream()
                .map(sucursal -> new SucursalResponse(
                        sucursal.getId(),
                        sucursal.getCodigo(),
                        sucursal.getNombre(),
                        sucursal.getDireccion(),
                        sucursal.getUbigeoCodigo(),
                        sucursal.getDepartamento(),
                        sucursal.getProvincia(),
                        sucursal.getDistrito(),
                        sucursal.getIgvPorcentaje(),
                        sucursal.getTipoOperacionDefaultId(),
                        sucursal.getTipoAfectacionDefaultId(),
                        sucursal.getTributoDefaultId(),
                        sucursal.getPorcentajeIgvDefault(),
                        sucursal.isActivo()
                ))
                .toList();
    }
}
