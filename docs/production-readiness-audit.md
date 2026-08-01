# Auditoria de concurrencia y resiliencia

Fecha: 2026-08-01

## Controles implementados

- Operaciones criticas de venta, compra, caja, abonos, inventario, notas y guias
  exigen `clientOperationId` y conservan un hash de la solicitud. Un reintento
  igual recupera el resultado; reutilizar la clave con otros datos se rechaza.
- Traslados de inventario bloquean y actualizan origen, destino y kardex en una
  sola transaccion. Un corte del navegador no deja un traslado parcial.
- La facturacion de ventas usa outbox persistente con lease renovable. Una tarea
  que espera en el executor no puede ser recuperada prematuramente por otra
  replica.
- Los jobs SUNAT tienen lock por tenant/documento, timeout menor que
  `retry_after`, backoff y reintento seguro del callback sin reenviar un
  documento terminal.
- Los listados historicos de ventas, compras, stock, kardex, turnos y CRM estan
  paginados o acotados. Dashboard y reportes CRM calculan agregados en SQL.
- Las migraciones fallidas bloquean solamente al tenant afectado con 503 y
  exponen la metrica `azurion.tenant.migration.failures`.
- Hikari, Tomcat, PHP-FPM, Horizon y descargas tienen limites y timeouts
  configurables. Los contenedores incluyen healthchecks y apagado gradual.

## Pendientes antes de declarar capacidad alta

### P1

1. `RegistrarNotaFiscalUseCase` y `RegistrarGuiaRemisionUseCase` aun llaman al
   facturador dentro de una transaccion. Deben migrarse a un outbox propio para
   que una caida externa no retenga conexiones Hikari.
2. La vista kanban `pipeline()` necesita paginacion por columna para tenants con
   decenas de miles de oportunidades; actualmente representa todas las tarjetas.
3. El POS sigue cargando el catalogo completo para busqueda local. Sustituirlo
   por autocomplete paginado cuando el catalogo pueda superar varios miles de
   productos.
4. En despliegue con varias replicas, archivos privados/publicos y los
   artefactos del facturador deben usar almacenamiento compartido. La
   configuracion ya permite seleccionar disco/ruta, pero la infraestructura debe
   provisionarlo.
5. Ejecutar `deploy/load/k6-read-paths.js` en staging con datos representativos.
   Sin esa medicion no existe un numero maximo de usuarios defendible.

### P2

- Dividir la vista CRM en mas chunks: su SCSS supera el budget por 29.97 kB y el
  chunk lazy ronda 899 kB.
- Cambiar `exceljs` por una carga/alternativa ESM si el tiempo de descarga de
  reportes se vuelve relevante.
- Validar los compose y healthchecks con Docker Compose en CI; Docker no estaba
  disponible en el equipo de esta auditoria.

## Criterios de salida

- Cero duplicados al repetir la misma clave de operacion con 2, 10 y 50
  solicitudes concurrentes.
- Cero stock negativo o traslados parciales durante cortes de red y reinicios.
- p95 menor a 1 segundo para lecturas y menor a 2 segundos para escrituras
  locales; las tareas SUNAT se miden por edad de cola.
- Hikari sin timeouts, PostgreSQL sin superar 80% de `max_connections`, colas
  sin crecimiento sostenido y cero reinicios por memoria durante la prueba.
- PDFs/XML/CDR descargables desde cualquier replica despues de reiniciar otra.
