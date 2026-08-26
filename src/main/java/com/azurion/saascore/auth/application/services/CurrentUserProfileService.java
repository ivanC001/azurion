package com.azurion.saascore.auth.application.services;

import com.azurion.saascore.auth.application.dto.ChangeCurrentUserPasswordRequest;
import com.azurion.saascore.auth.application.dto.CurrentUserProfileResponse;
import com.azurion.saascore.auth.application.dto.UpdateCurrentUserProfileRequest;
import com.azurion.saascore.auth.domain.entities.UsuarioGlobal;
import com.azurion.saascore.auth.domain.repositories.UsuarioGlobalRepository;
import com.azurion.saascore.auth.infrastructure.storage.UserProfilePhotoStorageService;
import com.azurion.multitenancy.TenantContext;
import com.azurion.saascore.usuarios.application.dto.UsuarioSucursalResponse;
import com.azurion.saascore.usuarios.application.services.UsuarioSucursalScopeService;
import com.azurion.saascore.usuarios.domain.entities.UsuarioTenant;
import com.azurion.saascore.usuarios.domain.repositories.UsuarioTenantRepository;
import com.azurion.security.jwt.TenantAuthenticationDetails;
import com.azurion.shared.exception.BusinessException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CurrentUserProfileService {

    private static final String PLATFORM_ACCOUNT = "Administracion de Azurion";
    private static final String TENANT_ACCOUNT = "Cuenta de empresa";

    private final UsuarioTenantRepository usuarioTenantRepository;
    private final UsuarioGlobalRepository usuarioGlobalRepository;
    private final UsuarioSucursalScopeService usuarioSucursalScopeService;
    private final PasswordEncoder passwordEncoder;
    private final UserProfilePhotoStorageService profilePhotoStorageService;

    @Transactional(readOnly = true)
    public CurrentUserProfileResponse get(Authentication authentication) {
        ProfileActor actor = requireActor(authentication);
        return actor.platformAccount()
                ? toPlatformResponse(findPlatformUser(actor.userId()), actor.roles())
                : toTenantResponse(findTenantUser(actor.userId()), actor.roles());
    }

    @Transactional
    public CurrentUserProfileResponse update(
            Authentication authentication,
            UpdateCurrentUserProfileRequest request
    ) {
        ProfileActor actor = requireActor(authentication);
        if (actor.platformAccount()) {
            throw new BusinessException(
                    "PERFIL_ADMINISTRATIVO_SOLO_LECTURA",
                    "El usuario y permisos de la administracion de Azurion se gestionan desde seguridad de plataforma."
            );
        }

        UsuarioTenant user = findTenantUser(actor.userId());
        validateEmailUnchanged(user.getEmail(), request.email());
        user.setNombres(request.nombres().trim());
        user.setApellidos(blankToNull(request.apellidos()));
        user.setTelefono(blankToNull(request.telefono()));
        user.setCargo(blankToNull(request.cargo()));
        UsuarioTenant saved = usuarioTenantRepository.save(user);
        return toTenantResponse(saved, actor.roles());
    }

    @Transactional
    public CurrentUserProfileResponse updatePhoto(Authentication authentication, MultipartFile file) {
        ProfileActor actor = requireEditableTenantActor(authentication);
        UsuarioTenant user = findTenantUser(actor.userId());
        String previousPhoto = user.getFotoPerfilUrl();
        String newPhoto = profilePhotoStorageService.store(TenantContext.getTenantId(), user.getId(), file);
        user.setFotoPerfilUrl(newPhoto);
        UsuarioTenant saved = usuarioTenantRepository.save(user);
        profilePhotoStorageService.deleteQuietly(previousPhoto);
        return toTenantResponse(saved, actor.roles());
    }

    @Transactional
    public CurrentUserProfileResponse deletePhoto(Authentication authentication) {
        ProfileActor actor = requireEditableTenantActor(authentication);
        UsuarioTenant user = findTenantUser(actor.userId());
        String previousPhoto = user.getFotoPerfilUrl();
        user.setFotoPerfilUrl(null);
        UsuarioTenant saved = usuarioTenantRepository.save(user);
        profilePhotoStorageService.deleteQuietly(previousPhoto);
        return toTenantResponse(saved, actor.roles());
    }

    @Transactional
    public void changePassword(Authentication authentication, ChangeCurrentUserPasswordRequest request) {
        ProfileActor actor = requireActor(authentication);
        String currentHash;
        if (actor.platformAccount()) {
            UsuarioGlobal user = findPlatformUser(actor.userId());
            currentHash = user.getPasswordHash();
            validateCurrentPassword(request.contrasenaActual(), currentHash);
            validateNewPassword(request.nuevaContrasena(), currentHash);
            user.setPasswordHash(passwordEncoder.encode(request.nuevaContrasena()));
            usuarioGlobalRepository.save(user);
            return;
        }

        UsuarioTenant user = findTenantUser(actor.userId());
        currentHash = user.getPasswordHash();
        validateCurrentPassword(request.contrasenaActual(), currentHash);
        validateNewPassword(request.nuevaContrasena(), currentHash);
        user.setPasswordHash(passwordEncoder.encode(request.nuevaContrasena()));
        usuarioTenantRepository.save(user);
    }

    private CurrentUserProfileResponse toTenantResponse(UsuarioTenant user, List<String> roles) {
        List<UsuarioSucursalResponse> branches = usuarioSucursalScopeService.findByUsuarioId(user.getId());
        return new CurrentUserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNombres(),
                user.getApellidos(),
                user.getEmail(),
                user.getTelefono(),
                user.getCargo(),
                user.getFotoPerfilUrl(),
                user.isActivo(),
                true,
                true,
                TENANT_ACCOUNT,
                roles,
                branches
        );
    }

    private CurrentUserProfileResponse toPlatformResponse(UsuarioGlobal user, List<String> roles) {
        return new CurrentUserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getUsername(),
                null,
                null,
                null,
                null,
                null,
                user.isActivo(),
                false,
                true,
                PLATFORM_ACCOUNT,
                roles,
                List.of()
        );
    }

    private UsuarioTenant findTenantUser(Long userId) {
        return usuarioTenantRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
    }

    private UsuarioGlobal findPlatformUser(Long userId) {
        return usuarioGlobalRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
    }

    private ProfileActor requireActor(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof TenantAuthenticationDetails details)) {
            throw BusinessException.unauthorized("AUTH_SESSION_INVALID", "No se pudo identificar la sesion actual");
        }
        List<String> roles = roleAuthorities(authentication.getAuthorities());
        boolean platformAccount = roles.contains("ADMIN_GENERAL") || roles.contains("PLATFORM_ADMIN");
        return new ProfileActor(details.getUserId(), platformAccount, roles);
    }

    private ProfileActor requireEditableTenantActor(Authentication authentication) {
        ProfileActor actor = requireActor(authentication);
        if (actor.platformAccount()) {
            throw new BusinessException(
                    "PERFIL_ADMINISTRATIVO_SOLO_LECTURA",
                    "El perfil de la administracion de Azurion se gestiona desde seguridad de plataforma."
            );
        }
        return actor;
    }

    private List<String> roleAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value -> value != null && value.startsWith("ROLE_"))
                .map(value -> value.replaceFirst("^ROLE_", ""))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private void validateEmailUnchanged(String currentEmail, String requestedEmail) {
        if (requestedEmail == null || requestedEmail.isBlank()) {
            return;
        }
        String current = blankToNull(currentEmail);
        String requested = requestedEmail.trim();
        if (current == null || !current.equalsIgnoreCase(requested)) {
            throw new BusinessException(
                    "PERFIL_CORREO_NO_EDITABLE",
                    "El correo de acceso no puede modificarse desde el perfil. Solicita el cambio a un administrador."
            );
        }
    }

    private void validateCurrentPassword(String currentPassword, String passwordHash) {
        if (!passwordEncoder.matches(currentPassword, passwordHash)) {
            throw new BusinessException("CONTRASENA_ACTUAL_INCORRECTA", "La contrasena actual no es correcta.");
        }
    }

    private void validateNewPassword(String newPassword, String currentHash) {
        if (passwordEncoder.matches(newPassword, currentHash)) {
            throw new BusinessException("CONTRASENA_SIN_CAMBIOS", "La nueva contrasena debe ser diferente a la actual.");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ProfileActor(Long userId, boolean platformAccount, List<String> roles) {
    }
}
