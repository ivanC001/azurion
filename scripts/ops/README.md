# Operaciones previas al despliegue

Scripts puntuales para el servidor. No forman parte del arranque de la
aplicacion: se ejecutan a mano y una sola vez.

## Colision de la version 96 de Flyway

**Cuando aplica:** solo si el arreglo de `productos` se aplico directamente en
el servidor como `V96__cotizaciones_productos_schema_alignment.sql`.

**Por que hay que hacer algo:** en el repositorio la version 96 ya estaba
ocupada por `V96__whatsapp_message_template_snapshot.sql`, asi que ese arreglo
se renumero a `V98__cotizaciones_productos_schema_alignment.sql`. Los esquemas
que tengan la version 96 registrada con la descripcion del arreglo veran que no
coincide con el script 96 del classpath.

`TenantMigrationService` llama a `migrate()` **sin** `repair()` automatico a
proposito: un historial alterado debe bloquear el alta en lugar de repararse
solo. Es decir, esto no se corrige por si mismo — aborta el arranque con una
`ValidateException`.

**Como se resuelve:**

```bash
psql "$DB_URL" -f scripts/ops/01_diagnostico_flyway_v96.sql   # solo lectura
```

Si no aparece ninguna linea `[COLISION]`, no hay nada que hacer: desplegar
directamente.

Si aparece alguna, respaldar el historial y reparar:

```bash
pg_dump -t '*.flyway_schema_history' "$DB_NAME" > flyway_history_backup.sql
psql "$DB_URL" -f scripts/ops/02_reparar_flyway_v96.sql
```

La reparacion solo borra filas del historial. Las columnas que creo la
migracion siguen en la base; `V98` las vuelve a declarar de forma idempotente
(`ADD COLUMN IF NOT EXISTS`), asi que no se pierde nada. El orden no importa
porque Flyway esta configurado con `outOfOrder(true)`.

## Que se aplica solo al desplegar

Nada de lo siguiente necesita intervencion manual. `TenantMigrationRunner`
migra en el arranque todos los tenants registrados segun sus modulos activos:

| Migracion | Que hace | Modulos que la reciben |
|---|---|---|
| `V96__whatsapp_message_template_snapshot` | Snapshot de plantillas de WhatsApp | CRM |
| `V97__repair_usuario_sucursal_scope` | Da alcance de sucursal a usuarios que se quedaron sin ninguna | CRM |
| `V98__cotizaciones_productos_schema_alignment` | `productos.almacen_id` y `precio_venta_modo` donde faltaban | INVENTARIO, VENTAS, COTIZACIONES |
| `V99__sucursal_ubigeo_optional_outside_peru` | `sucursales.ubigeo_codigo` deja de ser obligatorio | INVENTARIO, VENTAS, CAJA, COTIZACIONES, CRM |

Las cuatro son idempotentes: en un esquema que ya tenga el cambio no hacen
nada.

`TenantModuleMigrationPlannerTest` cubre dos invariantes que evitan que se
repita el hueco que provoco la incidencia original:

- todo modulo que deje `sucursales.ubigeo_codigo` en `NOT NULL` (V15 o V43)
  tiene que incluir tambien V99;
- todo modulo con la tabla `productos` (V2) tiene que poder responder por
  `almacen_id` y `precio_venta_modo`, sea por V8/V85 o por V98.
