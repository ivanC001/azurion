package com.azurion.saascore.caja.application.services;

import com.azurion.saascore.auth.application.services.AuthorizationService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CajaActorService {

    private final AuthorizationService authorizationService;
    private final UsuarioTenantRepository usuarioTenantRepository;

    public CajaActorService(AuthorizationService authorizationService,
                            UsuarioTenantRepository usuarioTenantRepository) {
        this.authorizationService = authorizationService;
        this.usuarioTenantRepository = usuarioTenantRepository;
    }

    public Actor actual() {
        Long usuarioId = authorizationService.currentUsuarioId();
        if (usuarioId != null) {
            UsuarioTenant usuario = usuarioTenantRepository.findById(usuarioId).orElse(null);
            if (usuario != null) {
                return new Actor(
                        usuarioId,
                        String.valueOf(usuarioId),
                        usuario.getNombres() == null || usuario.getNombres().isBlank()
                                ? usuario.getUsername()
                                : usuario.getNombres().trim()
                );
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null || authentication.getName() == null
                ? "system"
                : authentication.getName();
        return new Actor(null, username, username);
    }

    public record Actor(Long usuarioId, String referenciaId, String nombre) {
    }
}
