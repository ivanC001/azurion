package com.azurion.multitenancy;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MultiTenantJpaConfig implements HibernatePropertiesCustomizer {

    private final SchemaMultiTenantConnectionProvider multiTenantConnectionProvider;
    private final CurrentTenantResolver currentTenantResolver;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.multiTenancy", "SCHEMA");
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantResolver);
    }
}
