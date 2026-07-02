package com.azurion.saascore.configuracion.application.mappers;

import com.azurion.saascore.configuracion.application.dto.EmpresaModuloResponse;
import com.azurion.saascore.configuracion.domain.entities.EmpresaModulo;
import java.time.LocalDate;

public final class EmpresaModuloMapper {

    private EmpresaModuloMapper() {
    }

    public static EmpresaModuloResponse toResponse(EmpresaModulo empresaModulo) {
        LocalDate today = LocalDate.now();
        boolean vigente = empresaModulo.isActivo()
                && "ACTIVO".equalsIgnoreCase(empresaModulo.getEstado())
                && (empresaModulo.getFechaInicio() == null || !empresaModulo.getFechaInicio().isAfter(today))
                && (empresaModulo.getFechaFin() == null || !empresaModulo.getFechaFin().isBefore(today));

        return new EmpresaModuloResponse(
                empresaModulo.getId(),
                empresaModulo.getEmpresa().getId(),
                empresaModulo.getModulo().getId(),
                empresaModulo.getModulo().getCodigo(),
                empresaModulo.getModulo().getNombre(),
                empresaModulo.getEstado(),
                empresaModulo.isActivo(),
                empresaModulo.getFechaInicio(),
                empresaModulo.getFechaFin(),
                empresaModulo.getConfiguracionExtra(),
                vigente
        );
    }
}
