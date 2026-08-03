-- La arquitectura tributaria original clasifico estos permisos dentro de
-- CONFIGURACION. Los roles ERP creados despues se poblaron por el modulo
-- TRIBUTACION, por lo que ERP_ADMIN y ERP_CONTADOR no los heredaron.
UPDATE permisos
SET modulo = 'TRIBUTACION',
    updated_at = now()
WHERE codigo IN ('TRIBUTACION_READ', 'TRIBUTACION_WRITE')
  AND modulo IS DISTINCT FROM 'TRIBUTACION';

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.codigo IN ('ERP_ADMIN', 'ERP_CONTADOR')
  AND p.codigo IN ('TRIBUTACION_READ', 'TRIBUTACION_WRITE')
  AND p.activo = TRUE
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
