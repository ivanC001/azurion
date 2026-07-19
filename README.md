# AZURION Backend

Backend/API REST enterprise para ERP SaaS modular con Spring Boot 3, Java 21 y PostgreSQL MultiTenant por schema.

- Arquitectura completa: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Swagger: `http://localhost:8080/api/swagger-ui.html`

## Ejecutar local
1. `docker compose up -d postgres`
2. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)

## Primer administrador

No existen credenciales fijas activas. En el primer arranque configura temporalmente
`AZURION_BOOTSTRAP_ADMIN_USERNAME` y `AZURION_BOOTSTRAP_ADMIN_PASSWORD` (minimo 16
caracteres). Tras verificar el acceso, retira ambas variables. Los siguientes
administradores se crean desde el endpoint protegido `/api/v1/auth/register`.

## Login
Administrador de plataforma: `POST /api/v1/auth/public/login`.

Usuario de empresa: `POST /api/v1/auth/tenant/login`.

```json
{
  "username": "platform.admin",
  "password": "una-clave-segura"
}
```

## Facturacion integrada (Azurion -> Facturador)

- Flujo: `POST /api/v1/saas/cajas/{id}/ventas`
- Azurion guarda la tarea en una cola transaccional, responde al usuario y la procesa con reintentos persistentes.
- Ticket de venta se registra y responde sin espera SUNAT.

Variables relevantes:

- `FACTURADOR_BASE_URL`
- `FACTURADOR_API_KEY`
- `FACTURADOR_WAIT_PROCESSED_ENABLED` (default `true`)
- `FACTURADOR_WAIT_PROCESSED_TIMEOUT_MS` (default `90000`)
- `FACTURADOR_WAIT_PROCESSED_POLL_INTERVAL_MS` (default `1500`)
