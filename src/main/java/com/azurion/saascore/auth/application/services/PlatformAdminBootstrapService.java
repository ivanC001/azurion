package com.azurion.saascore.auth.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAdminBootstrapService implements ApplicationRunner {

    private final UsuarioGlobalRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${azurion.security.bootstrap-admin.username:}")
    private String configuredUsername;

    @Value("${azurion.security.bootstrap-admin.password:}")
    private String configuredPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String username = configuredUsername == null ? "" : configuredUsername.trim();
        String password = configuredPassword == null ? "" : configuredPassword;
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new BusinessException(
                    "BOOTSTRAP_ADMIN_CONFIG_INCOMPLETA",
                    "AZURION_BOOTSTRAP_ADMIN_USERNAME y AZURION_BOOTSTRAP_ADMIN_PASSWORD deben configurarse juntos"
            );
        }
        if (!username.matches("[A-Za-z0-9._@-]{3,120}")) {
            throw new BusinessException("BOOTSTRAP_ADMIN_USERNAME_INVALIDO", "El usuario bootstrap no tiene un formato valido");
        }
        if (password.length() < 16 || password.length() > 128) {
            throw new BusinessException(
                    "BOOTSTRAP_ADMIN_PASSWORD_INVALIDO",
                    "La clave bootstrap debe tener entre 16 y 128 caracteres"
            );
        }

        UsuarioGlobal user = userRepository.findByUsernameIgnoreCase(username).orElseGet(UsuarioGlobal::new);
        if (user.getId() != null && user.isActivo()) {
            log.info("El administrador bootstrap {} ya existe y esta activo; no se modifico su clave", username);
            return;
        }
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles("ROLE_PLATFORM_ADMIN,ROLE_ADMIN_GENERAL");
        user.setEmpresaDefault(TenantContext.DEFAULT_TENANT);
        user.setActivo(true);
        userRepository.save(user);
        log.warn("Administrador bootstrap {} creado o reactivado. Retira las variables BOOTSTRAP del entorno", username);
    }
}
