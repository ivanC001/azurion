package com.azurion.saascore.empresas.application.usecases;

import com.azurion.saascore.empresas.application.dto.CreateEmpresaRegistrationRequest;
import com.azurion.saascore.empresas.application.dto.CreateEmpresaRequest;
import com.azurion.saascore.empresas.application.dto.EmpresaRegistrationResponse;
import com.azurion.saascore.empresas.application.dto.EmpresaResponse;
import com.azurion.saascore.planes.domain.entities.Plan;
import com.azurion.saascore.planes.domain.repositories.PlanModuloRepository;
import com.azurion.saascore.planes.domain.repositories.PlanRepository;
import com.azurion.saascore.suscripciones.application.dto.CreateSuscripcionRequest;
import com.azurion.saascore.suscripciones.application.dto.SuscripcionResponse;
import com.azurion.saascore.suscripciones.application.usecases.CreateSuscripcionUseCase;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateEmpresaRegistrationUseCase {

    private final PlanRepository planRepository;
    private final PlanModuloRepository planModuloRepository;
    private final CreateEmpresaUseCase createEmpresaUseCase;
    private final CreateSuscripcionUseCase createSuscripcionUseCase;

    @Transactional
    public EmpresaRegistrationResponse execute(CreateEmpresaRegistrationRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new BusinessException("PLAN_NOT_FOUND", "No se encontro el plan seleccionado"));
        if (!"ACTIVO".equalsIgnoreCase(plan.getEstado())) {
            throw new BusinessException("PLAN_NO_DISPONIBLE", "El plan seleccionado no esta activo");
        }

        List<String> moduleCodes = planModuloRepository.findModuloCodigosByPlanId(plan.getId());
        if (moduleCodes.isEmpty()) {
            throw new BusinessException("PLAN_SIN_MODULOS", "Configura al menos un modulo en el plan antes de asignarlo");
        }

        EmpresaResponse empresa = createEmpresaUseCase.execute(new CreateEmpresaRequest(
                request.ruc(),
                request.razonSocial(),
                request.tipoDocumentoFiscal(),
                request.nombreComercial(),
                request.paisCodigo(),
                request.paisNombre(),
                request.monedaCodigo(),
                request.monedaSimbolo(),
                request.zonaHoraria(),
                request.idioma(),
                request.tenantId(),
                request.schemaName(),
                moduleCodes
        ));

        SuscripcionResponse suscripcion = createSuscripcionUseCase.execute(new CreateSuscripcionRequest(
                empresa.id(),
                plan.getId(),
                request.fechaInicio(),
                request.limiteUsuarios()
        ));
        return new EmpresaRegistrationResponse(empresa, suscripcion);
    }
}
