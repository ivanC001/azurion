package com.azurion.saascore.almacenes.application.usecases;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAlmacenUseCase {

    private final AlmacenRepository almacenRepository;
    private final SucursalRepository sucursalRepository;
    private final SucursalOperationalGuard sucursalOperationalGuard;

    public AlmacenResponse execute(CreateAlmacenRequest request) {
        almacenRepository.findByCodigo(request.codigo()).ifPresent(existing -> {
            throw new BusinessException("ALMACEN_DUPLICADO", "Ya existe un almacen con ese codigo");
        });
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        sucursalOperationalGuard.requireActive(sucursal);

        Almacen almacen = new Almacen();
        almacen.setCodigo(request.codigo());
        almacen.setNombre(request.nombre());
        almacen.setDireccion(request.direccion());
        almacen.setSucursal(sucursal);
        almacen.setActivo(true);

        Almacen saved = almacenRepository.save(almacen);
        return toResponse(saved);
    }

    private AlmacenResponse toResponse(Almacen almacen) {
        return new AlmacenResponse(
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                almacen.getDireccion(),
                almacen.getSucursal().getId(),
                almacen.getSucursal().getCodigo(),
                almacen.getSucursal().getNombre(),
                almacen.getTipoAlmacen(),
                almacen.getEstado(),
                almacen.isActivo()
        );
    }
}
