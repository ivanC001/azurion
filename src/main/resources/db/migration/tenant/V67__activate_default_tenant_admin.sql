-- Regulariza exclusivamente la cuenta semilla creada por V7.
-- Las cuentas con una contrasena personalizada no se modifican.
UPDATE usuarios
SET password_hash = '$2a$10$aIszUFzr4lzmpNdmg7WD6unu5Yvec5ChfVvUqNkScV2396EvCoxvO',
    activo = TRUE,
    updated_at = now()
WHERE username = 'admin'
  AND password_hash = '$2a$10$MTNVJ2WKlIqmYk5.tfZ5cefPT3u8z3NYJ.hM4LvDELoFrWyylhNP6';
