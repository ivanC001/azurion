package com.azurion.shared.persistence;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.util.Collection;
import java.util.Objects;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serializes business keys across application instances for the duration of
 * the surrounding PostgreSQL transaction. Non-PostgreSQL test profiles skip
 * the database-specific lock.
 */
@Service
@RequiredArgsConstructor
public class BusinessOperationLockService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BusinessOperationLockService.class);
    private final EntityManager entityManager;
    private final DataSource dataSource;
    private volatile Boolean postgresql;

    public void lockAll(Collection<String> lockKeys) {
        if (!isPostgresql()) {
            return;
        }
        lockKeys.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .distinct()
                .sorted()
                .forEach(this::lock);
    }

    private void lock(String lockKey) {
        entityManager.createNativeQuery(
                        "select pg_advisory_xact_lock(hashtextextended(cast(:lockKey as text), 0))"
                )
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }

    private boolean isPostgresql() {
        Boolean cached = postgresql;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (postgresql != null) {
                return postgresql;
            }
            try (Connection connection = dataSource.getConnection()) {
                postgresql = connection.getMetaData().getDatabaseProductName()
                        .toLowerCase()
                        .contains("postgresql");
            } catch (Exception exception) {
                log.warn("No se pudo detectar el motor SQL para los locks de operaciones", exception);
                postgresql = false;
            }
            return postgresql;
        }
    }
}
