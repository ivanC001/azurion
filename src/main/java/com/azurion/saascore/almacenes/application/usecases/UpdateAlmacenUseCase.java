package com.azurion.saascore.almacenes.application.usecases;

import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.application.dto.UpdateAlmacenRequest;
import com.azurion.saascore.almacenes.application.mappers.AlmacenMapper;
import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.inventory.domain.repositories.StockRepository;
import com.azurion.shared.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAlmacenUseCase {

    private final AlmacenRepository almacenRepository;
    private final SucursalRepository sucursalRepository;
    private final SucursalOperationalGuard sucursalOperationalGuard;
    private final StockRepository stockRepository;

    @Transactional
    public AlmacenResponse execute(Long almacenId, UpdateAlmacenRequest request) {
        Almacen almacen = almacenRepository.findById(almacenId)
                .orElseThrow(() -> new BusinessException(
                        "ALMACEN_NO_ENCONTRADO",
                        "Almacen no encontrado"
                ));
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new BusinessException(
                        "SUCURSAL_NO_ENCONTRADA",
                        "Sucursal no encontrada"
                ));
        sucursalOperationalGuard.requireActive(sucursal);

        boolean activo = request.activo() == null ? almacen.isActivo() : request.activo();
        BigDecimal existencias = stockRepository.sumCantidadByAlmacenId(almacenId);
        if (existencias.compareTo(BigDecimal.ZERO) != 0
                && (!activo || !almacen.getSucursal().getId().equals(sucursal.getId()))) {
            throw new BusinessException(
                    "ALMACEN_CON_STOCK",
                    "Traslada las existencias antes de desactivar el almacen o cambiarlo de sucursal"
            );
        }
        String tipoAlmacen = normalizeTipo(request.tipoAlmacen(), almacen.getTipoAlmacen());
        if (activo && "PRINCIPAL".equals(tipoAlmacen)
                && almacenRepository.existsBySucursalIdAndTipoAlmacenIgnoreCaseAndActivoTrueAndIdNot(
                        sucursal.getId(),
                        "PRINCIPAL",
                        almacenId
                )) {
            throw new BusinessException(
                    "ALMACEN_PRINCIPAL_DUPLICADO",
                    "La sucursal ya tiene un almacen principal activo"
            );
        }
        almacen.setNombre(request.nombre().trim());
        almacen.setDireccion(trim(request.direccion()));
        almacen.setSucursal(sucursal);
        almacen.setTipoAlmacen(tipoAlmacen);
        almacen.setPermiteVenta(
                request.permiteVenta() == null ? almacen.isPermiteVenta() : request.permiteVenta()
        );
        almacen.setActivo(activo);
        almacen.setEstado(activo ? "ACTIVO" : "INACTIVO");
        return AlmacenMapper.toResponse(almacenRepository.save(almacen));
    }

    private String normalizeTipo(String value, String fallback) {
        String normalized = trim(value);
        if (normalized == null) {
            return fallback;
        }
        normalized = normalized.toUpperCase();
        return switch (normalized) {
            case "PRINCIPAL", "SECUNDARIO", "TRANSITO", "DEVOLUCIONES" -> normalized;
            default -> throw new BusinessException(
                    "TIPO_ALMACEN_INVALIDO",
                    "Use PRINCIPAL, SECUNDARIO, TRANSITO o DEVOLUCIONES"
            );
        };
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
