-- Reejecuta de forma idempotente la regularizacion del administrador semilla.
-- Es necesaria para tenants cuyo historial ya uso la version 67 para otra migracion.
UPDATE usuarios
SET password_hash = '$2a$10$aIszUFzr4lzmpNdmg7WD6unu5Yvec5ChfVvUqNkScV2396EvCoxvO',
    activo = TRUE,
    updated_at = now()
WHERE username = 'admin'
  AND password_hash = '$2a$10$MTNVJ2WKlIqmYk5.tfZ5cefPT3u8z3NYJ.hM4LvDELoFrWyylhNP6';
