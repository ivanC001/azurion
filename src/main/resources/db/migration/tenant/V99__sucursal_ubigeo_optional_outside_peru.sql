-- Permite sucursales sin ubigeo para tenants fuera de Peru.
--
-- El ubigeo es un codigo del catalogo de SUNAT y solo tiene sentido en Peru.
-- V15 (ERP) y V43 (CRM) dejan la columna NOT NULL y siembran '150101 LIMA' como
-- sede base, asi que un tenant de otro pais quedaba con un domicilio peruano
-- inventado y no podia registrar sucursales reales sin elegir un distrito de
-- Peru.
--
-- La obligatoriedad para Peru no se relaja: la aplica SucursalLocationResolver,
-- que es quien conoce el pais del tenant (la tabla empresas vive en el esquema
-- public y no puede referenciarse desde una constraint del esquema del tenant).
--
-- Idempotente: DROP NOT NULL sobre una columna ya nullable no hace nada, y la
-- guarda evita fallar en un esquema que todavia no tenga la tabla.

DO $$
BEGIN
    IF to_regclass('sucursales') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE sucursales
        ALTER COLUMN ubigeo_codigo DROP NOT NULL;
END $$;
