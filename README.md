# AZURION Backend

Backend/API REST enterprise para ERP SaaS modular con Spring Boot 3, Java 21 y PostgreSQL MultiTenant por schema.

- Arquitectura completa: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Swagger: `http://localhost:8080/api/swagger-ui.html`

## Ejecutar local
1. `docker compose up -d postgres`
2. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)

## Credenciales iniciales
- Usuario: `platform.admin`
- Password (hash seeded): `password`

## Login
`POST /api/v1/auth/login`

```json
{
  "username": "platform.admin",
  "password": "password",
  "tenantId": "public"
}
```

## Facturacion integrada (Azurion -> Facturador)

- Flujo: `POST /api/v1/saas/cajas/{id}/ventas`
- Azurion emite al facturador y, para factura/boleta, espera estado SUNAT final (`ACEPTADO`/`RECHAZADO`/`ERROR`) antes de responder.
- Ticket de venta se registra y responde sin espera SUNAT.

Variables relevantes:

- `FACTURADOR_BASE_URL`
- `FACTURADOR_API_KEY`
- `FACTURADOR_WAIT_PROCESSED_ENABLED` (default `true`)
- `FACTURADOR_WAIT_PROCESSED_TIMEOUT_MS` (default `90000`)
- `FACTURADOR_WAIT_PROCESSED_POLL_INTERVAL_MS` (default `1500`)
