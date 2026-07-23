package com.azurion.saascore.auth.application.usecases;

import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.auth.application.dto.AuthEmpresaResponse;
import com.azurion.saascore.auth.application.dto.LoginRequest;
import com.azurion.saascore.auth.application.dto.LoginResponse;
import com.azurion.saascore.auth.application.dto.TenantLoginResponse;
import com.azurion.saascore.auth.application.services.EffectivePermissionService;
import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import com.azurion.saascore.modulos.application.services.ModuleAccessService;
import com.azurion.saascore.usuarios.application.services.RoleCodeSupport;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioGlobalRolRepository;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.security.jwt.JwtProperties;
import com.azurion.security.jwt.JwtTokenProvider;
import com.azurion.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UsuarioGlobalRepository userRepository;
    private final UsuarioGlobalRolRepository usuarioGlobalRolRepository;
    private final UsuarioTenantRepository usuarioTenantRepository;
    private final EmpresaRepository empresaRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final EffectivePermissionService effectivePermissionService;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;
    private final ModuleAccessService moduleAccessService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse executePublic(LoginRequest request) {
        TenantContext.setTenantId(TenantContext.DEFAULT_TENANT);

        UsuarioGlobal user = userRepository.findByUsernameAndActivoTrue(request.username())
                .orElseThrow(this::badCredentials);
        validatePassword(request.password(), user.getPasswordHash());

        LinkedHashSet<String> roles = resolveGlobalRoles(user);
        List<String> roleList = List.copyOf(roles);

        String token = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getId(),
                TenantContext.DEFAULT_TENANT,
                roleList,
                List.of(),
                List.of()
        );

        return new LoginResponse(
                token,
                "Bearer",
                jwtProperties.expiration().toSeconds(),
                user.getUsername(),
                TenantContext.DEFAULT_TENANT,
                roleList,
                List.of(),
                List.of(),
                roles.contains("ROLE_ADMIN_GENERAL"),
                false,
                OffsetDateTime.now()
        );
    }

    public TenantLoginResponse executeTenant(LoginRequest request) {
        String tenant = resolveTenantForTenantLogin(request.tenantId());

        TenantContext.setTenantId(tenant);

        if (!TenantContext.DEFAULT_TENANT.equalsIgnoreCase(tenant)) {
            UsuarioTenant tenantUser = usuarioTenantRepository.findWithUsuarioRolesByUsernameIgnoreCase(request.username())
                    .filter(UsuarioTenant::isActivo)
                    .orElseThrow(this::badCredentials);
            validatePassword(request.password(), tenantUser.getPasswordHash());

            tenantUser.setUltimoAcceso(java.time.LocalDateTime.now());
            usuarioTenantRepository.save(tenantUser);

            LinkedHashSet<String> tenantRoles = tenantUser.getUsuarioRoles().stream()
                    .map(usuarioRol -> RoleCodeSupport.toAuthority(usuarioRol.getRol().getCodigo()))
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

            List<String> roleList = List.copyOf(tenantRoles);
            Empresa empresa = empresaRepository.findByTenantId(tenant)
                    .orElseThrow(() -> new BusinessException("TENANT_NO_ENCONTRADO", "Empresa no encontrada para el tenant"));
            List<String> modules = moduleAccessService.getActiveModules(empresa.getId());
            List<String> permissions = effectivePermissionService.findPermissionCodes(tenantUser.getId(), modules);
            String token = jwtTokenProvider.generateToken(
                    tenantUser.getUsername(),
                    tenantUser.getId(),
                    tenant,
                    roleList,
                    permissions,
                    modules
            );

            return new TenantLoginResponse(
                    token,
                    "Bearer",
                    jwtProperties.expiration().toSeconds(),
                    tenantUser.getUsername(),
                    tenantUser.getId(),
                    tenantUser.getNombres(),
                    tenantUser.getEmail(),
                    tenant,
                    toEmpresaResponse(empresa),
                    roleList,
                    permissions,
                    modules,
                    usuarioSucursalScopeService.findByUsuarioId(tenantUser.getId()),
                    tenantRoles.contains("ROLE_ADMIN_EMPRESA"),
                    OffsetDateTime.now()
            );
        }

        throw badCredentials();
    }

    private String resolveTenantForTenantLogin(String tenantOrRuc) {
        String value = tenantOrRuc;
        if (value == null || value.isBlank()) {
            value = TenantContext.getTenantId();
        }
        if (value == null || value.isBlank() || TenantContext.DEFAULT_TENANT.equalsIgnoreCase(value)) {
            throw new BusinessException("TENANT_INVALIDO", "El identificador fiscal o tenant de la empresa es obligatorio");
        }

        String normalized = value.trim();
        var empresaByFiscalId = empresaRepository.findByRucIgnoreCase(normalized);
        if (empresaByFiscalId.isPresent()) {
            return empresaByFiscalId.get().getTenantId();
        }

        return normalized;
    }

    private LinkedHashSet<String> resolveGlobalRoles(UsuarioGlobal user) {
        LinkedHashSet<String> roles = Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        usuarioGlobalRolRepository.findByUsuarioGlobalIdAndActivoTrue(user.getId())
                .forEach(role -> roles.add(RoleCodeSupport.toAuthority(role.getRolCodigo())));

        if (roles.contains("ROLE_ADMIN_GENERAL")) {
            roles.add("ROLE_PLATFORM_ADMIN");
        }
        return roles;
    }

    private void validatePassword(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw badCredentials();
        }
    }

    private BadCredentialsException badCredentials() {
        return new BadCredentialsException("Invalid username or password");
    }

    private AuthEmpresaResponse toEmpresaResponse(Empresa empresa) {
        return new AuthEmpresaResponse(
                empresa.getId(),
                empresa.getRuc(),
                empresa.getRazonSocial(),
                empresa.getTenantId(),
                empresa.getSchemaName(),
                empresa.getLogoPanelUrl(),
                empresa.isActivo()
        );
    }
}
