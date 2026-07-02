ALTER TABLE crm_catalogo_items
    DROP CONSTRAINT IF EXISTS chk_crm_catalogo_tipo;

ALTER TABLE crm_catalogo_items
    ADD CONSTRAINT chk_crm_catalogo_tipo CHECK (
        tipo_item IN (
            'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
            'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
            'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
            'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
        )
    );

ALTER TABLE crm_prospectos
    DROP CONSTRAINT IF EXISTS chk_crm_prospectos_tipo_interes;

ALTER TABLE crm_prospectos
    ADD CONSTRAINT chk_crm_prospectos_tipo_interes CHECK (
        tipo_interes IN (
            'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
            'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
            'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
            'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
        )
    );

ALTER TABLE crm_oportunidades
    DROP CONSTRAINT IF EXISTS chk_crm_oportunidades_tipo;

ALTER TABLE crm_oportunidades
    ADD CONSTRAINT chk_crm_oportunidades_tipo CHECK (
        tipo_oportunidad IN (
            'PRODUCTO', 'SERVICIO', 'VEHICULO', 'INMUEBLE', 'PROYECTO', 'CURSO',
            'SEGURO', 'SOFTWARE', 'MARKETING', 'CLINICA', 'JURIDICO', 'TURISMO',
            'MAQUINARIA', 'FINANCIERO', 'EDUCACION', 'HOSPITALIDAD', 'MANUFACTURA',
            'TELECOMUNICACION', 'ENERGIA', 'AGRICULTURA', 'CONSULTORIA', 'OTRO'
        )
    );
