INSERT INTO stock (
    producto_id,
    almacen_id,
    cantidad,
    stock_reservado,
    stock_minimo,
    estado,
    created_at,
    updated_at,
    version
)
SELECT
    p.id,
    p.almacen_id,
    0,
    0,
    0,
    'ACTIVO',
    now(),
    now(),
    0
FROM productos p
WHERE p.almacen_id IS NOT NULL
  AND COALESCE(p.maneja_stock, TRUE) = TRUE
  AND UPPER(COALESCE(p.tipo_producto, 'PRODUCTO')) <> 'SERVICIO'
  AND NOT EXISTS (
      SELECT 1
      FROM stock s
      WHERE s.producto_id = p.id
        AND s.almacen_id = p.almacen_id
  );
