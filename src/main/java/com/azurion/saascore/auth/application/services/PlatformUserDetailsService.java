package com.azurion.saascore.auth.application.services;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.usuarios.application.services.RoleCodeSupport;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformUserDetailsService implements UserDetailsService {

    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final UsuarioTenantRepository usuarioTenantRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String tenant = TenantContext.getTenantId();
        if (!TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenant)) {
            UsuarioTenant tenantUser = usuarioTenantRepository.findWithUsuarioRolesByUsernameIgnoreCase(username)
                    .filter(UsuarioTenant::isActivo)
                    .orElse(null);
            if (tenantUser != null) {
                List<String> roles = tenantUser.getUsuarioRoles().stream()
                        .map(usuarioRol -> RoleCodeSupport.toAuthority(usuarioRol.getRol().getCodigo()))
                        .distinct()
                        .toList();

                return new User(
                        tenantUser.getUsername(),
                        tenantUser.getPasswordHash(),
                        roles.stream().map(SimpleGrantedAuthority::new).toList()
                );
            }
        }

        UsuarioGlobal user = usuarioGlobalRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new User(
                user.getUsername(),
                user.getPasswordHash(),
                Arrays.stream(user.getRoles().split(","))
                        .map(String::trim)
                        .filter(role -> !role.isBlank())
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }
}
