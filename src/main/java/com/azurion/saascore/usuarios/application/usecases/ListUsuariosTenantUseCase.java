package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.usuarios.application.dto.UsuarioTenantResponse;
import com.azurion.saascore.usuarios.application.mappers.UsuariosMapper;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListUsuariosTenantUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;

    public List<UsuarioTenantResponse> execute() {
        return usuarioTenantRepository.findAllByOrderByNombresAsc().stream()
                .map(usuario -> UsuariosMapper.toResponse(usuario, usuarioSucursalScopeService.findByUsuarioId(usuario.getId())))
                .toList();
    }
}
