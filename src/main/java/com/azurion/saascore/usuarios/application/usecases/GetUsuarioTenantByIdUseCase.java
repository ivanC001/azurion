package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.usuarios.application.dto.UsuarioTenantResponse;
import com.azurion.saascore.usuarios.application.mappers.UsuariosMapper;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetUsuarioTenantByIdUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;

    public UsuarioTenantResponse execute(Long id) {
        UsuarioTenant usuario = usuarioTenantRepository.findWithUsuarioRolesById(id)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        return UsuariosMapper.toResponse(usuario, usuarioSucursalScopeService.findByUsuarioId(usuario.getId()));
    }
}
