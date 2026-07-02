package com.azurion.saascore.empresas.application.usecases;

import com.azurion.multitenancy.TenantProvisioningService;
import com.azurion.saascore.configuracion.domain.entities.EmpresaModulo;
import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.application.dto.CreateEmpresaRequest;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.domain.entities.Modulo;
import com.azurion.saascore.modulos.domain.repositories.ModuloRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateEmpresaUseCase {

    private final EmpresaRepository empresaRepository;
    private final TenantProvisioningService tenantProvisioningService;
    private final ModuloRepository moduloRepository;
    private final EmpresaModuloRepository empresaModuloRepository;

    @Transactional
    public EmpresaResponse execute(CreateEmpresaRequest request) {
        validateEmpresaDoesNotExist(request);
        List<String> normalizedModuleCodes = validateAndNormalizeModules(request.moduloCodigos());

        tenantProvisioningService.createTenantSchema(request.tenantId(), request.schemaName(), normalizedModuleCodes);

        Empresa empresa = new Empresa();
        empresa.setRuc(request.ruc());
        empresa.setRazonSocial(request.razonSocial());
        empresa.setTenantId(request.tenantId());
        empresa.setSchemaName(request.schemaName());
        empresa.setActivo(true);

        Empresa saved = empresaRepository.save(empresa);
        syncInitialModules(saved, normalizedModuleCodes);

        return new EmpresaResponse(
                saved.getId(),
                saved.getRuc(),
                saved.getRazonSocial(),
                saved.getTenantId(),
                saved.getSchemaName(),
                saved.getLogoPanelUrl(),
                saved.isActivo()
        );
    }

    private void validateEmpresaDoesNotExist(CreateEmpresaRequest request) {
        empresaRepository.findByRuc(request.ruc()).ifPresent(existing -> {
            throw new BusinessException("EMPRESA_RUC_EXISTS", "Ya existe una empresa con RUC: " + request.ruc());
        });
        empresaRepository.findByTenantId(request.tenantId()).ifPresent(existing -> {
            throw new BusinessException("EMPRESA_TENANT_EXISTS", "Ya existe una empresa con tenant: " + request.tenantId());
        });
        empresaRepository.findBySchemaName(request.schemaName()).ifPresent(existing -> {
            throw new BusinessException("EMPRESA_SCHEMA_EXISTS", "Ya existe una empresa con schema: " + request.schemaName());
        });
    }

    private List<String> validateAndNormalizeModules(List<String> moduloCodigos) {
        LinkedHashSet<String> normalizedCodes = normalizeModuleCodes(moduloCodigos);
        if (normalizedCodes.isEmpty()) {
            throw new BusinessException("MODULOS_REQUIRED", "Selecciona al menos un modulo para registrar la empresa.");
        }

        Map<String, Modulo> modulosByCodigo = loadModulesByCodigo();
        List<String> missingCodes = normalizedCodes.stream()
                .filter(code -> !modulosByCodigo.containsKey(code))
                .toList();

        if (!missingCodes.isEmpty()) {
            throw new BusinessException("MODULO_NOT_FOUND", "Modulos no registrados en plataforma: " + String.join(", ", missingCodes));
        }

        return List.copyOf(normalizedCodes);
    }

    private void syncInitialModules(Empresa empresa, List<String> moduloCodigos) {
        LinkedHashSet<String> normalizedCodes = normalizeModuleCodes(moduloCodigos);

        if (normalizedCodes.isEmpty()) {
            return;
        }

        Map<String, Modulo> modulosByCodigo = loadModulesByCodigo();

        List<EmpresaModulo> assignments = normalizedCodes.stream()
                .map(code -> buildModuleAssignment(empresa, code, modulosByCodigo))
                .toList();

        empresaModuloRepository.saveAll(assignments);
    }

    private EmpresaModulo buildModuleAssignment(Empresa empresa, String moduloCodigo, Map<String, Modulo> modulosByCodigo) {
        Modulo modulo = modulosByCodigo.get(moduloCodigo);
        if (modulo == null) {
            throw new BusinessException("MODULO_NOT_FOUND", "Modulo not found: " + moduloCodigo);
        }

        EmpresaModulo empresaModulo = new EmpresaModulo();
        empresaModulo.setEmpresa(empresa);
        empresaModulo.setModulo(modulo);
        empresaModulo.setActivo(true);
        empresaModulo.setEstado("ACTIVO");
        empresaModulo.setFechaInicio(LocalDate.now());
        return empresaModulo;
    }

    private LinkedHashSet<String> normalizeModuleCodes(List<String> moduloCodigos) {
        if (moduloCodigos == null) {
            return new LinkedHashSet<>();
        }

        return moduloCodigos.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeModuleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeModuleCode(String value) {
        String code = value.trim().toUpperCase(Locale.ROOT);
        return switch (code) {
            case "FACTURACION_CORE" -> "FACTURACION";
            case "SAAS_CORE" -> "ERP";
            default -> code;
        };
    }

    private Map<String, Modulo> loadModulesByCodigo() {
        return moduloRepository.findAllByOrderByNombreAsc().stream()
                .collect(Collectors.toMap(
                        modulo -> modulo.getCodigo().trim().toUpperCase(Locale.ROOT),
                        Function.identity()
                ));
    }
}
