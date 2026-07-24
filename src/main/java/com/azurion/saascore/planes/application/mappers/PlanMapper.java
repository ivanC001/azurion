package com.azurion.saascore.planes.application.mappers;

import com.azurion.saascore.planes.application.dto.PlanResponse;
import com.azurion.saascore.planes.domain.entities.Plan;
import java.util.List;

public final class PlanMapper {

    private PlanMapper() {
    }

    public static PlanResponse toResponse(Plan plan) {
        return toResponse(plan, List.of());
    }

    public static PlanResponse toResponse(Plan plan, List<String> moduloCodigos) {
        return new PlanResponse(
                plan.getId(),
                plan.getNombre(),
                plan.getCodigo(),
                plan.getDescripcion(),
                plan.getLimiteMensualBolsa(),
                plan.getLimiteUsuarios(),
                plan.getPrecioMensual(),
                plan.getEstado(),
                moduloCodigos
        );
    }
}
