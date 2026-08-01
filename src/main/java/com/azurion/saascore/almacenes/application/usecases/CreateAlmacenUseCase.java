package com.azurion.saascore.almacenes.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.almacenes.application.dto.AlmacenResponse;
import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.application.mappers.AlmacenMapper;
import com.azurion.saascore.almacenes.application.services.OperationalCodeGenerator;
import com.azurion.saascore.almacenes.domain.entities.Almacen;
import com.azurion.saascore.almacenes.domain.repositories.AlmacenRepository;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.sucursales.application.services.SucursalOperationalGuard;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.persistence.BusinessOperationLockService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAlmacenUseCase {

    private final AlmacenRepository almacenRepository;
    private final SucursalRepository sucursalRepository;
    private final SucursalOperationalGuard sucursalOperationalGuard;
    private final OperationalCodeGenerator codeGenerator;
    private final BusinessOperationLockService operationLockService;

    @Transactional
    public AlmacenResponse execute(CreateAlmacenRequest request) {
        Sucursal sucursal = sucursalRepository.findById(request.sucursalId())
                .orElseThrow(() -> new BusinessException("SUCURSAL_NO_ENCONTRADA", "Sucursal no encontrada"));
        sucursalOperationalGuard.requireActive(sucursal);
        operationLockService.lockAll(List.of(
                "warehouse-create:" + TenantContext.getTenantId() + ":" + sucursal.getId()
        ));
        String tipoAlmacen = normalizeTipo(request.tipoAlmacen(), sucursal.getId());
        validatePrincipalDisponible(sucursal.getId(), tipoAlmacen);

        String requestedCode = trim(request.codigo());
        String codigo = requestedCode == null
                ? codeGenerator.nextAlmacenCode(sucursal)
                : requestedCode.toUpperCase();
        almacenRepository.findByCodigoIgnoreCase(codigo).ifPresent(existing -> {
            throw new BusinessException("ALMACEN_DUPLICADO", "Ya existe un almacen con ese codigo");
        });

        Almacen almacen = new Almacen();
        almacen.setCodigo(codigo);
        almacen.setNombre(resolveNombre(request.nombre(), sucursal, tipoAlmacen));
        almacen.setDireccion(resolveDireccion(request.direccion(), sucursal));
        almacen.setSucursal(sucursal);
        almacen.setTipoAlmacen(tipoAlmacen);
        almacen.setPermiteVenta(resolvePermiteVenta(request.permiteVenta(), tipoAlmacen));
        almacen.setEstado("ACTIVO");
        almacen.setActivo(true);

        Almacen saved = almacenRepository.save(almacen);
        return AlmacenMapper.toResponse(saved);
    }

    private String normalizeTipo(String value, Long sucursalId) {
        String normalized = trim(value);
        if (normalized == null) {
            return almacenRepository.existsBySucursalIdAndTipoAlmacenIgnoreCaseAndActivoTrue(
                    sucursalId,
                    "PRINCIPAL"
            ) ? "SECUNDARIO" : "PRINCIPAL";
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

    private void validatePrincipalDisponible(Long sucursalId, String tipoAlmacen) {
        if ("PRINCIPAL".equals(tipoAlmacen)
                && almacenRepository.existsBySucursalIdAndTipoAlmacenIgnoreCaseAndActivoTrue(
                        sucursalId,
                        "PRINCIPAL"
                )) {
            throw new BusinessException(
                    "ALMACEN_PRINCIPAL_DUPLICADO",
                    "La sucursal ya tiene un almacen principal activo"
            );
        }
    }

    private String resolveNombre(String value, Sucursal sucursal, String tipoAlmacen) {
        String nombre = trim(value);
        if (nombre != null) {
            return nombre;
        }
        String tipo = switch (tipoAlmacen) {
            case "SECUNDARIO" -> "secundario";
            case "TRANSITO" -> "de transito";
            case "DEVOLUCIONES" -> "de devoluciones";
            default -> "principal";
        };
        return "Almacen " + tipo + " - " + sucursal.getNombre();
    }

    private String resolveDireccion(String value, Sucursal sucursal) {
        String direccion = trim(value);
        return direccion == null ? trim(sucursal.getDireccion()) : direccion;
    }

    private boolean resolvePermiteVenta(Boolean value, String tipoAlmacen) {
        if (value != null) {
            return value;
        }
        return !"TRANSITO".equals(tipoAlmacen) && !"DEVOLUCIONES".equals(tipoAlmacen);
    }

    private String trim(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
