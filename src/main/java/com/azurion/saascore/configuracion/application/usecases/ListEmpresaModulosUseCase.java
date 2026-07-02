package com.azurion.saascore.configuracion.application.usecases;

import com.azurion.saascore.configuracion.application.dto.EmpresaModuloResponse;
import com.azurion.saascore.configuracion.application.mappers.EmpresaModuloMapper;
import com.azurion.saascore.configuracion.application.services.EmpresaModuloAdminAccessService;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListEmpresaModulosUseCase {

    private final EmpresaModuloRepository empresaModuloRepository;
    private final EmpresaRepository empresaRepository;
    private final ModuloRepository moduloRepository;
    private final EmpresaModuloAdminAccessService accessService;

    public List<EmpresaModuloResponse> execute(Long empresaId) {
        accessService.requireViewAccess(empresaId);

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new com.azurion.shared.exception.BusinessException("EMPRESA_NOT_FOUND", "Empresa not found: " + empresaId));

        Map<Long, com.azurion.saascore.configuracion.domain.entities.EmpresaModulo> assigned = empresaModuloRepository.findDetailedByEmpresaId(empresaId)
                .stream()
                .collect(Collectors.toMap(item -> item.getModulo().getId(), Function.identity()));

        return moduloRepository.findAllByOrderByNombreAsc().stream()
                .map(modulo -> assigned.containsKey(modulo.getId())
                        ? EmpresaModuloMapper.toResponse(assigned.get(modulo.getId()))
                        : buildInactiveResponse(empresa, modulo))
                .toList();
    }

    private EmpresaModuloResponse buildInactiveResponse(Empresa empresa, Modulo modulo) {
        return new EmpresaModuloResponse(
                null,
                empresa.getId(),
                modulo.getId(),
                modulo.getCodigo(),
                modulo.getNombre(),
                "INACTIVO",
                false,
                null,
                null,
                null,
                false
        );
    }
}
