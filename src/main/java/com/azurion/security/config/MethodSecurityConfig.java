package com.azurion.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(
        prefix = "azurion.security",
        name = "disable-auth-for-dev",
        havingValue = "false",
        matchIfMissing = true
)
public class MethodSecurityConfig {
}
