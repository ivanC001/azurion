package com.azurion.saascore.sucursales.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.almacenes.application.dto.CreateAlmacenRequest;
import com.azurion.saascore.almacenes.application.services.OperationalCodeGenerator;
import com.azurion.saascore.almacenes.application.usecases.CreateAlmacenUseCase;
import com.azurion.saascore.sucursales.application.dto.CreateSucursalRequest;
import com.azurion.saascore.sucursales.application.dto.SucursalResponse;
import com.azurion.saascore.sucursales.domain.entities.Sucursal;
import com.azurion.saascore.sucursales.domain.repositories.SucursalRepository;
import com.azurion.saascore.sucursales.application.services.SucursalLocationResolver;
import com.azurion.saascore.sucursales.application.services.SucursalLocationResolver.SucursalLocation;
import com.azurion.shared.exception.BusinessException;
import com.azurion.shared.persistence.BusinessOperationLockService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSucursalUseCase {

    private final SucursalRepository sucursalRepository;
    private final SucursalLocationResolver locationResolver;
    private final OperationalCodeGenerator codeGenerator;
    private final CreateAlmacenUseCase createAlmacenUseCase;
    private final BusinessOperationLockService operationLockService;

    @Transactional
    public SucursalResponse execute(CreateSucursalRequest request) {
        operationLockService.lockAll(List.of("branch-create:" + TenantContext.getTenantId()));
        String requestedCode = trim(request.codigo());
        String codigo = requestedCode == null
                ? codeGenerator.nextSucursalCode()
                : requestedCode.toUpperCase();
        if (sucursalRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new BusinessException("SUCURSAL_DUPLICADA", "Ya existe una sucursal con ese codigo");
        }

        SucursalLocation location = locationResolver.resolve(
                request.ubigeoCodigo(),
                request.departamento(),
                request.provincia(),
                request.distrito()
        );

        BigDecimal igvPorcentaje = request.igvPorcentaje();
        if (igvPorcentaje.compareTo(BigDecimal.ZERO) < 0 || igvPorcentaje.compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessException("IGV_INVALIDO", "El IGV debe estar entre 0 y 100");
        }

        Sucursal sucursal = new Sucursal();
        sucursal.setCodigo(codigo);
        sucursal.setNombre(request.nombre().trim());
        sucursal.setDireccion(trim(request.direccion()));
        sucursal.setUbigeoCodigo(location.ubigeoCodigo());
        sucursal.setDepartamento(location.departamento());
        sucursal.setProvincia(location.provincia());
        sucursal.setDistrito(location.distrito());
        sucursal.setIgvPorcentaje(igvPorcentaje);
        applyTaxConfiguration(sucursal, igvPorcentaje);
        sucursal.setActivo(true);

        Sucursal saved = sucursalRepository.save(sucursal);
        if (Boolean.TRUE.equals(request.crearAlmacenPrincipal())) {
            createAlmacenUseCase.execute(new CreateAlmacenRequest(
                    null,
                    "Almacen principal - " + saved.getNombre(),
                    saved.getDireccion(),
                    saved.getId(),
                    "PRINCIPAL",
                    true
            ));
        }
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
