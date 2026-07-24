package com.azurion.saascore.usuarios.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.suscripciones.domain.entities.Suscripcion;
import com.azurion.saascore.suscripciones.domain.repositories.SuscripcionRepository;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.saascore.usuarios.application.dto.TenantUserQuotaResponse;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantUserLimitService {

    private final EmpresaRepository empresaRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final UsuarioTenantRepository usuarioTenantRepository;

    /**
     * The subscription row is locked until the surrounding transaction finishes.
     * This serializes concurrent creates/reactivations for the same tenant.
     */
    public void assertCanActivateAnotherUser() {
        String tenantId = TenantContext.getTenantId();
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "TENANT_NO_ENCONTRADO",
                        "No se encontro la empresa del tenant"
                ));
        Suscripcion suscripcion = suscripcionRepository.findActiveForUpdate(empresa.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "PLAN_NO_CONFIGURADO",
                        "El tenant no tiene una suscripcion activa"
                ));

        long activos = usuarioTenantRepository.countByActivoTrue();
        int limite = effectiveLimit(suscripcion);
        if (activos >= limite) {
            throw new BusinessException(
                    "LIMITE_USUARIOS_PLAN_ALCANZADO",
                    "El plan permite hasta " + limite
                            + " usuario(s) activos. Deshabilita uno o amplia el plan."
            );
        }
    }

    public TenantUserQuotaResponse currentQuota() {
        String tenantId = TenantContext.getTenantId();
        Empresa empresa = empresaRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException(
                        "TENANT_NO_ENCONTRADO",
                        "No se encontro la empresa del tenant"
                ));
        Suscripcion suscripcion = suscripcionRepository.findActive(empresa.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "PLAN_NO_CONFIGURADO",
                        "El tenant no tiene una suscripcion activa"
                ));
        long activeUsers = usuarioTenantRepository.countByActivoTrue();
        int limit = effectiveLimit(suscripcion);
        return new TenantUserQuotaResponse(
                activeUsers,
                limit,
                Math.max(0, limit - activeUsers),
                suscripcion.getPlan().getCodigo()
        );
    }

    private int effectiveLimit(Suscripcion suscripcion) {
        return suscripcion.getLimiteUsuarios() == null
                ? suscripcion.getPlan().getLimiteUsuarios()
                : suscripcion.getLimiteUsuarios();
    }
}
