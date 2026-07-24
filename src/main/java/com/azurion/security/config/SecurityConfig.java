package com.azurion.security.config;

import com.azurion.multitenancy.TenantFilter;
import com.azurion.security.jwt.JwtAuthenticationFilter;
import com.azurion.security.ratelimit.RateLimitFilter;
import com.azurion.shared.audit.AuditTrailFilter;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final TenantFilter tenantFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AuditTrailFilter auditTrailFilter;

    @Value("${azurion.multitenancy.public-endpoints:/v1/auth/public/login,/v1/auth/tenant/login,/v1/auth/session/replace,/v1/public/crm/leads,/public/crm/leads,/v1/public/crm/catalogo/**,/public/crm/catalogo/**,/v1/public/crm/whatsapp/**,/public/crm/whatsapp/**,/v1/public/crm/meta/**,/public/crm/meta/**,/v1/facturador/callback/**,/actuator/health,/files/**}")
    private String publicEndpointsProperty;

    @Value("${azurion.security.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
    private String corsAllowedOriginsProperty;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"code\":\"AUTH_ERROR\",\"message\":\"Debes iniciar sesion para continuar\",\"details\":[],\"userActionable\":true}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"code\":\"ACCESS_DENIED\",\"message\":\"No tienes permiso para realizar esta operacion\",\"details\":[],\"userActionable\":true}");
                        })
                );

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(resolvePublicEndpoints()).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, RateLimitFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, TenantFilter.class)
                .addFilterAfter(auditTrailFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = Arrays.stream(corsAllowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(allowedOrigins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Authorization", "X-Correlation-Id"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String[] resolvePublicEndpoints() {
        return Arrays.stream(publicEndpointsProperty.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
    }
}
