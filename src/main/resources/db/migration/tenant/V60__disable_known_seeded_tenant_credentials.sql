-- A fixed credential must never remain active after migrations finish.
-- Custom passwords are intentionally left untouched.
UPDATE usuarios
SET activo = FALSE
WHERE username = 'admin'
  AND password_hash = '$2a$10$MTNVJ2WKlIqmYk5.tfZ5cefPT3u8z3NYJ.hM4LvDELoFrWyylhNP6';
