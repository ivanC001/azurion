package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.usuarios.application.dto.UpdateUsuarioTenantRequest;
import com.azurion.saascore.usuarios.application.dto.UsuarioTenantResponse;
import com.azurion.saascore.usuarios.application.mappers.UsuariosMapper;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUsuarioTenantUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;

    @Transactional
    public UsuarioTenantResponse execute(Long id, UpdateUsuarioTenantRequest request) {
        UsuarioTenant usuario = usuarioTenantRepository.findWithUsuarioRolesById(id)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        usuario.setNombres(request.nombres().trim());
        usuario.setEmail(request.email() == null ? null : request.email().trim());
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }

        UsuarioTenant saved = usuarioTenantRepository.save(usuario);
        usuarioSucursalScopeService.sync(saved.getId(), request.sucursalIds());
        return UsuariosMapper.toResponse(saved, usuarioSucursalScopeService.findByUsuarioId(saved.getId()));
    }
}
