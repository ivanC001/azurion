package com.azurion.saascore.suscripciones.application.usecases;

import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.saascore.suscripciones.application.dto.CreateSuscripcionRequest;
import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.mappers.SuscripcionMapper;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.shared.exception.BusinessException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateSuscripcionUseCase {

    private final SuscripcionRepository suscripcionRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanRepository planRepository;

    @Transactional
    public SuscripcionResponse execute(CreateSuscripcionRequest request) {
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new BusinessException("EMPRESA_NOT_FOUND", "Empresa not found: " + request.empresaId()));

        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", "Plan not found: " + request.planId()));
        if (!"ACTIVO".equalsIgnoreCase(plan.getEstado())) {
            throw new BusinessException("PLAN_NO_DISPONIBLE", "El plan seleccionado no esta activo");
        }
        if (!suscripcionRepository.findAllActiveStateForUpdate(empresa.getId()).isEmpty()) {
            throw new BusinessException(
                    "SUSCRIPCION_ACTIVA_EXISTENTE",
                    "La empresa ya tiene una suscripcion activa; actualiza su plan actual"
            );
        }

        Suscripcion suscripcion = new Suscripcion();
        suscripcion.setEmpresa(empresa);
        suscripcion.setPlan(plan);
        suscripcion.setEstado("ACTIVA");
        suscripcion.setFechaInicio(request.fechaInicio() == null ? LocalDate.now() : request.fechaInicio());
        suscripcion.setLimiteUsuarios(request.limiteUsuarios());

        return SuscripcionMapper.toResponse(suscripcionRepository.save(suscripcion));
    }
}
