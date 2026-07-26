package com.azurion.saascore.crm.application.services;

import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.util.Collection;
import java.util.Objects;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Serializes public lead identities across every application instance.
 * PostgreSQL advisory locks live until the surrounding transaction finishes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmIngressLockService {

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
            } catch (Exception ex) {
                log.warn("No se pudo detectar el motor SQL para los locks de leads", ex);
                postgresql = false;
            }
            return postgresql;
        }
    }
}
