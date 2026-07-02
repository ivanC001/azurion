package com.azurion.saascore.auth.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.application.dto.RegisterAdminGeneralRequest;
import com.azurion.saascore.auth.application.dto.RegisterAdminGeneralResponse;
import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterAdminGeneralUseCase {

    private static final String ADMIN_GENERAL_ROLE = "ROLE_ADMIN_GENERAL";

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterAdminGeneralResponse execute(RegisterAdminGeneralRequest request) {
        String username = request.username().trim();
        if (usuarioGlobalRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("USUARIO_DUPLICADO", "Ya existe un usuario global con ese username");
        }

        UsuarioGlobal usuario = new UsuarioGlobal();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRoles(ADMIN_GENERAL_ROLE);
        usuario.setEmpresaDefault(TenantContext.DEFAULT_TENANT);
        usuario.setActivo(true);

        UsuarioGlobal saved = usuarioGlobalRepository.save(usuario);
        return new RegisterAdminGeneralResponse(
                saved.getId(),
                saved.getUsername(),
                TenantContext.DEFAULT_TENANT,
                List.of(ADMIN_GENERAL_ROLE)
        );
    }
}
