# Prueba de concurrencia

Esta prueba k6 cubre las rutas de lectura que usa el panel con mayor frecuencia.
Debe ejecutarse contra un ambiente de staging con datos similares a produccion y
un usuario exclusivo para carga.

```powershell
$env:BASE_URL = 'https://staging.example.com/api'
$env:TENANT_ID = 'empresa_prueba'
$env:ACCESS_TOKEN = '<token-del-usuario-de-carga>'
$env:VUS = '25'
$env:DURATION = '2m'
k6 run deploy/load/k6-read-paths.js
```

Subir `VUS` por escalones (25, 50, 100, 200) y detenerse cuando se incumpla
alguno de los umbrales. Durante la prueba se deben observar al menos:

- `hikaricp_connections_active`, pendientes y timeouts;
- CPU, memoria, pausas GC y reinicios del backend;
- latencia y bloqueos de PostgreSQL;
- longitud y antiguedad de las colas Horizon/outbox;
- errores 429, 5xx y p95/p99 por endpoint.

El ultimo escalon que cumple los umbrales durante un periodo sostenido es la
capacidad validada de esa configuracion concreta; no es un limite universal del
codigo.

## Dimensionamiento de conexiones

No aumentes hilos HTTP sin revisar PostgreSQL. Para el backend usa como regla:

```text
(replicas API * DB_POOL_MAX) + conexiones de migracion/administracion
    <= max_connections de PostgreSQL - reserva operativa
```

Conserva al menos 10 conexiones de reserva (o 10% si es mayor). Si se despliegan
3 replicas con `DB_POOL_MAX=20`, PostgreSQL debe poder atender esas 60 conexiones,
las del facturador y la reserva. La saturacion debe producir espera corta o 503,
no una cola ilimitada de solicitudes.

El resultado solo es valido si la prueba incluye simultaneamente lecturas,
ventas, movimientos de stock, descarga de comprobantes y procesamiento de las
colas. Ejecuta tambien una prueba de corte de red y confirma que el mismo
`clientOperationId` recupera la operacion original sin duplicar stock, caja ni
documentos fiscales.
