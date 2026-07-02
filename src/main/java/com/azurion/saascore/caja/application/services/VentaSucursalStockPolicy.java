package com.azurion.saascore.caja.application.services;

import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.caja.domain.entities.Caja;
import com.azurion.shared.exception.BusinessException;
import java.util.Collection;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VentaSucursalStockPolicy {

    private final AlmacenRepository almacenRepository;

    public void validar(Caja caja, Collection<Long> almacenIds) {
        if (caja == null || caja.getSucursal() == null) {
            throw new BusinessException(
                    "CAJA_SIN_SUCURSAL",
                    "La caja debe pertenecer a una sucursal para registrar ventas"
            );
        }
        if (!caja.getSucursal().isActivo()) {
            throw new BusinessException(
                    "SUCURSAL_INACTIVA",
                    "La sucursal de la caja esta inactiva y no permite ventas"
            );
        }

        Long sucursalId = caja.getSucursal().getId();
        for (Long almacenId : almacenIds) {
            Almacen almacen = almacenRepository.findById(almacenId)
                    .orElseThrow(() -> new BusinessException(
                            "ALMACEN_NO_ENCONTRADO",
                            "Almacen no encontrado: " + almacenId
                    ));

            if (!almacen.isActivo() || !"ACTIVO".equalsIgnoreCase(almacen.getEstado())) {
                throw new BusinessException(
                        "ALMACEN_INACTIVO",
                        "No se puede vender desde un almacen inactivo: " + almacen.getNombre()
                );
            }

            if (almacen.getSucursal() == null || !Objects.equals(almacen.getSucursal().getId(), sucursalId)) {
                throw new BusinessException(
                        "STOCK_OTRA_SUCURSAL",
                        "El stock de " + almacen.getNombre() + " pertenece a otra sucursal y solo puede consultarse"
                );
            }
        }
    }
}
