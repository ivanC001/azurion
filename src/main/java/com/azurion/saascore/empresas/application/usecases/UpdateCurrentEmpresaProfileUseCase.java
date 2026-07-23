package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.application.dto.UpdateCurrentEmpresaProfileRequest;
import com.azurion.saascore.empresas.application.mappers.EmpresaMapper;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCurrentEmpresaProfileUseCase {

    private final EmpresaRepository empresaRepository;
    private final GetCurrentEmpresaUseCase getCurrentEmpresaUseCase;

    @Transactional
    public EmpresaResponse execute(UpdateCurrentEmpresaProfileRequest request) {
        Empresa empresa = getCurrentEmpresaUseCase.resolveCurrentEmpresa();
        String fiscalId = upper(request.ruc());
        empresaRepository.findByRucIgnoreCase(fiscalId)
                .filter(existing -> !existing.getId().equals(empresa.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("EMPRESA_DOCUMENTO_FISCAL_EXISTS", "El identificador fiscal ya pertenece a otra empresa");
                });

        String zoneId = required(request.zonaHoraria());
        try {
            ZoneId.of(zoneId);
        } catch (DateTimeException exception) {
            throw new BusinessException("EMPRESA_ZONA_HORARIA_INVALIDA", "Selecciona una zona horaria valida");
        }

        empresa.setRuc(fiscalId);
        empresa.setRazonSocial(required(request.razonSocial()));
        empresa.setTipoDocumentoFiscal(upper(request.tipoDocumentoFiscal()));
        empresa.setNombreComercial(trim(request.nombreComercial()));
        empresa.setDireccionFiscal(trim(request.direccionFiscal()));
        empresa.setDistrito(trim(request.distrito()));
        empresa.setProvincia(trim(request.provincia()));
        empresa.setDepartamento(trim(request.departamento()));
        empresa.setPaisCodigo(upper(request.paisCodigo()));
        empresa.setPaisNombre(required(request.paisNombre()));
        empresa.setCorreoPrincipal(lower(request.correoPrincipal()));
        empresa.setTelefono(trim(request.telefono()));
        empresa.setCelular(trim(request.celular()));
        empresa.setSitioWeb(trim(request.sitioWeb()));
        empresa.setFacebook(trim(request.facebook()));
        empresa.setInstagram(trim(request.instagram()));
        empresa.setRepresentanteNombre(trim(request.representanteNombre()));
        empresa.setRepresentanteTipoDocumento(upperNullable(request.representanteTipoDocumento()));
        empresa.setRepresentanteNumeroDocumento(trim(request.representanteNumeroDocumento()));
        empresa.setRepresentanteCargo(trim(request.representanteCargo()));
        empresa.setRepresentanteCorreo(lower(request.representanteCorreo()));
        empresa.setRepresentanteTelefono(trim(request.representanteTelefono()));
        empresa.setZonaHoraria(zoneId);
        empresa.setIdioma(required(request.idioma()));
        empresa.setFormatoFecha(upper(request.formatoFecha()));
        empresa.setFormatoHora(upper(request.formatoHora()));
        empresa.setMonedaCodigo(upper(request.monedaCodigo()));
        empresa.setMonedaSimbolo(required(request.monedaSimbolo()));
        return EmpresaMapper.toResponse(empresaRepository.save(empresa));
    }

    private String required(String value) {
        String normalized = trim(value);
        if (normalized == null) {
            throw new BusinessException("EMPRESA_DATO_REQUERIDO", "Completa los datos obligatorios de la empresa");
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upper(String value) {
        return required(value).toUpperCase(Locale.ROOT);
    }

    private String upperNullable(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String lower(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
