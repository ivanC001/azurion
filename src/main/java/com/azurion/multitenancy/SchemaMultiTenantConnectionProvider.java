package com.azurion.multitenancy;

import com.azurion.shared.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;
    private final TenantSchemaLookupService schemaLookupService;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        try {
            String schema = schemaLookupService.resolveSchema(tenantIdentifier);
            connection.setSchema(schema);
            return connection;
        } catch (SQLException ex) {
            releaseAnyConnection(connection);
            throw new BusinessException("TENANT_SCHEMA_ERROR", "Unable to switch schema: " + tenantIdentifier);
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            connection.setSchema(TenantContext.DEFAULT_TENANT);
        } finally {
            releaseAnyConnection(connection);
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Not supported");
    }

    @PreDestroy
    public void shutdown() {
        log.info("SchemaMultiTenantConnectionProvider closed");
    }
}
