package com.azurion.saascore.empresas.application.mappers;

import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.empresas.domain.entities.Empresa;

public final class EmpresaMapper {

    private EmpresaMapper() {
    }

    public static EmpresaResponse toResponse(Empresa empresa) {
        return new EmpresaResponse(
                empresa.getId(),
                empresa.getRuc(),
                empresa.getRazonSocial(),
                empresa.getTipoDocumentoFiscal(),
                empresa.getNombreComercial(),
                empresa.getDireccionFiscal(),
                empresa.getDistrito(),
                empresa.getProvincia(),
                empresa.getDepartamento(),
                empresa.getPaisCodigo(),
                empresa.getPaisNombre(),
                empresa.getCorreoPrincipal(),
                empresa.getTelefono(),
                empresa.getCelular(),
                empresa.getSitioWeb(),
                empresa.getFacebook(),
                empresa.getInstagram(),
                empresa.getRepresentanteNombre(),
                empresa.getRepresentanteTipoDocumento(),
                empresa.getRepresentanteNumeroDocumento(),
                empresa.getRepresentanteCargo(),
                empresa.getRepresentanteCorreo(),
                empresa.getRepresentanteTelefono(),
                empresa.getZonaHoraria(),
                empresa.getIdioma(),
                empresa.getFormatoFecha(),
                empresa.getFormatoHora(),
                empresa.getMonedaCodigo(),
                empresa.getMonedaSimbolo(),
                empresa.getTenantId(),
                empresa.getSchemaName(),
                empresa.getLogoPanelUrl(),
                empresa.isActivo()
        );
    }
}
