package com.azurion.multitenancy;

import com.azurion.saascore.configuracion.domain.repositories.EmpresaModuloRepository;
import com.azurion.saascore.empresas.domain.entities.Empresa;
import com.azurion.saascore.empresas.domain.repositories.EmpresaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantMigrationService {

    private final DataSource dataSource;
    private final EmpresaRepository empresaRepository;
    private final EmpresaModuloRepository empresaModuloRepository;
    private final TenantModuleMigrationPlanner migrationPlanner;
    private final TenantMigrationReadiness migrationReadiness;

    public void migrateSchema(String schemaName) {
        migrateLegacySchema(schemaName);
    }

    public void migrateSchema(
            String schemaName,
            List<String> requestedModules,
            boolean legacyFallbackWhenEmpty
    ) {
        TenantMigrationPlan plan =
                migrationPlanner.buildPlan(requestedModules, legacyFallbackWhenEmpty);
        if (plan.legacyFullMigration()) {
            migrateLegacySchema(schemaName);
            return;
        }

        migrateSchemaScripts(schemaName, plan.scriptNames());
    }

    public void migrateTenant(String tenantId, String schemaName) {
        Empresa empresa = empresaRepository.findByTenantId(tenantId).orElse(null);
        if (empresa == null) {
            migrateLegacySchema(schemaName);
            return;
        }

        boolean hasAssignments = empresaModuloRepository.countByEmpresaId(empresa.getId()) > 0;
        List<String> activeModules = hasAssignments
                ? empresaModuloRepository.findActiveModuleCodes(empresa.getId(), LocalDate.now())
                : List.of();

        migrateSchema(schemaName, activeModules, !hasAssignments);
    }

    public void migrateSchemas(List<TenantSchemaRegistry> registries) {
        for (TenantSchemaRegistry registry : registries) {
            try {
                log.info(
                        "Running tenant migrations for tenant={} schema={}",
                        registry.getTenantId(),
                        registry.getSchemaName()
                );
                migrateTenant(registry.getTenantId(), registry.getSchemaName());
                migrationReadiness.markReady(registry.getTenantId());
            } catch (Exception ex) {
                migrationReadiness.markFailed(registry.getTenantId(), ex);
                log.error(
                        "Tenant migration failed for tenant={} schema={}",
                        registry.getTenantId(),
                        registry.getSchemaName(),
                        ex
                );
            }
        }
    }

    private void migrateLegacySchema(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .load();
        migrateWithoutRepair(flyway);
    }

    private void migrateSchemaScripts(String schemaName, List<String> scriptNames) {
        if (scriptNames.isEmpty()) {
            log.info("No tenant module migrations pending for schema={}", schemaName);
            return;
        }

        Path tempDirectory = null;
        try {
            tempDirectory = Files.createTempDirectory("azurion-tenant-migrations-");
            for (String scriptName : scriptNames) {
                copyMigrationScript(scriptName, tempDirectory);
            }

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schemaName)
                    .locations("filesystem:" + tempDirectory.toAbsolutePath())
                    .baselineOnMigrate(true)
                    .outOfOrder(true)
                    .ignoreMigrationPatterns("*:missing")
                    .load();
            migrateWithoutRepair(flyway);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No se pudieron preparar las migraciones modulares del tenant",
                    ex
            );
        } finally {
            deleteTempDirectory(tempDirectory);
        }
    }

    private void migrateWithoutRepair(Flyway flyway) {
        // migrate() valida el historial aplicado y luego ejecuta las migraciones pendientes.
        // No se ejecuta repair automaticamente: un historial alterado debe bloquear el alta.
        flyway.migrate();
    }

    private void copyMigrationScript(String scriptName, Path tempDirectory) throws IOException {
        ClassPathResource resource =
                new ClassPathResource("db/migration/tenant/" + scriptName);
        if (!resource.exists()) {
            throw new IllegalStateException("Migration script not found: " + scriptName);
        }

        Path target = tempDirectory.resolve(scriptName);
        @Cleanup InputStream inputStream = resource.getInputStream();
        Files.copy(inputStream, target);
    }

    private void deleteTempDirectory(Path tempDirectory) {
        if (tempDirectory == null) {
            return;
        }

        try {
            Files.list(tempDirectory).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("Could not delete temporary tenant migration file {}", path, ex);
                }
            });
            Files.deleteIfExists(tempDirectory);
        } catch (IOException ex) {
            log.warn(
                    "Could not delete temporary tenant migration directory {}",
                    tempDirectory,
                    ex
            );
        }
    }
}
