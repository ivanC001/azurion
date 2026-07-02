package com.azurion.saascore.usuarios.application.usecases;

import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteUsuarioTenantUseCase {

    private final UsuarioTenantRepository usuarioTenantRepository;

    @Transactional
    public void execute(Long id) {
        UsuarioTenant usuario = usuarioTenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        usuarioTenantRepository.delete(usuario);
    }
}
