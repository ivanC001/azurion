UPDATE almacenes
SET sucursal_id = (
    SELECT id
    FROM sucursales
    ORDER BY id
    LIMIT 1
)
WHERE sucursal_id IS NULL;

ALTER TABLE almacenes
    ALTER COLUMN sucursal_id SET NOT NULL;
