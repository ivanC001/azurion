package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.usuarios.application.dto.UpdateUsuarioPasswordRequest;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateUsuarioTenantPasswordUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(Long id, UpdateUsuarioPasswordRequest request) {
        UsuarioTenant usuario = usuarioTenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuarioTenantRepository.save(usuario);
    }
}
