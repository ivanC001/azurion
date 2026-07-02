WITH categorias_base(nombre, descripcion, estado) AS (
VALUES
    ('Alimentos y bebidas', 'Productos comestibles, bebidas y abarrotes', 'ACTIVO'),
    ('Ropa y accesorios', 'Prendas, calzado y accesorios', 'ACTIVO'),
    ('Repuestos', 'Repuestos automotrices, industriales y similares', 'ACTIVO'),
    ('Ferreteria', 'Herramientas, materiales y productos de ferreteria', 'ACTIVO'),
    ('Salud y cuidado personal', 'Farmacia, higiene y cuidado personal', 'ACTIVO'),
    ('Hogar y limpieza', 'Productos para hogar, limpieza y mantenimiento', 'ACTIVO'),
    ('Tecnologia y electronica', 'Equipos, componentes y accesorios tecnologicos', 'ACTIVO'),
    ('Servicios', 'Servicios ofrecidos por la empresa', 'ACTIVO'),
    ('Otros', 'Productos sin una categoria especifica', 'ACTIVO')
)
INSERT INTO categorias (nombre, descripcion, estado)
SELECT base.nombre, base.descripcion, base.estado
FROM categorias_base base
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias existente
    WHERE UPPER(existente.nombre) = UPPER(base.nombre)
      AND existente.padre_id IS NULL
);
