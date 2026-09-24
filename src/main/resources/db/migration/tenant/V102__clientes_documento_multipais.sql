-- Clientes de CRM fuera de Peru usan documentos propios (CURP, RFC, CC, NIT, ...).
ALTER TABLE clientes ALTER COLUMN tipo_documento TYPE VARCHAR(30);
ALTER TABLE clientes ALTER COLUMN numero_documento TYPE VARCHAR(30);
