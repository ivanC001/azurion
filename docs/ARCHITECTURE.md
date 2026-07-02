# AZURION Backend Enterprise Blueprint

## 1) Vision
AZURION is a Modular Monolith on Spring Boot 3 / Java 21 focused on SaaS ERP core.

- In scope: multi-tenant SaaS administration and ERP transactional modules.
- Out of scope in this service: electronic invoicing internals (XML, SUNAT, CDR, digital signatures).
- Frontend is external (Angular, separate repository).

## 2) Modular structure
```text
com.azurion
+-- shared
+-- multitenancy
+-- security
+-- infrastructure
+-- saascore
    +-- auth
    +-- empresas
    +-- planes
    +-- modulos
    +-- suscripciones
    +-- configuracion (empresa_modulos)
    +-- inventory
    +-- clientes
    +-- ventas
    +-- roles
```

Each module follows:
- `domain`
- `application`
- `infrastructure` (when needed)
- `presentation`

## 3) MultiTenant model (PostgreSQL schema per company)
Runtime flow:
`JWT -> tenantId claim -> TenantContext -> schema lookup (public.schemas_empresas) -> connection.setSchema(schema_name)`

Core components:
- `TenantContext`
- `TenantFilter`
- `CurrentTenantResolver`
- `SchemaMultiTenantConnectionProvider`
- `TenantSchemaLookupService`
- `TenantProvisioningService`

## 4) Security
- Stateless JWT with Spring Security.
- Method-level authorization enabled (`@EnableMethodSecurity`).
- Public endpoints only:
  - `POST /api/v1/auth/login`
  - OpenAPI/Swagger paths
  - `GET /api/actuator/health`, `GET /api/actuator/info`
- All other endpoints require JWT.

## 5) Database strategy
### Public schema
- `empresas`
- `planes`
- `modulos`
- `suscripciones`
- `empresa_modulos`
- `usuarios_globales`
- `schemas_empresas`
- `auditoria_global`

### Tenant schema
- `productos`, `almacenes`, `stock`, `kardex_movimientos`
- `clientes`
- `ventas`
- `roles`, `permisos`, `rol_permisos`

## 6) Implemented SaaS APIs
- Auth: `/api/v1/auth/*`
- Empresas: `/api/v1/saas/empresas/*`
- Planes: `/api/v1/saas/planes/*`
- Modulos: `/api/v1/saas/modulos/*`
- Suscripciones: `/api/v1/saas/suscripciones/*`
- Empresa modulos: `/api/v1/saas/empresas/{empresaId}/modulos/*`
- Inventory: `/api/v1/saas/inventory/*`
- Clientes: `/api/v1/saas/clientes/*`
- Ventas: `/api/v1/saas/ventas/*`
- Roles y permisos: `/api/v1/saas/roles/*`, `/api/v1/saas/permisos/*`

Swagger:
- `/api/swagger-ui.html`
- `/api/v3/api-docs`

## 7) Events and decoupling with FACTURADOR
- SaaS core emits contracts/events (for example `SaleRegisteredEvent`).
- `facturacioncore` components in this repo are disabled by default using:
  - `azurion.facturacion.enabled=false`
- This keeps SaaS core ready to publish events to external FACTURADOR via Kafka/RabbitMQ later.

## 8) Observability and enterprise concerns
- Correlation ID filter (`X-Correlation-Id`).
- Audit trail persistence (`public.auditoria_global`).
- MDC fields: tenant/user/trace.
- Global exception handler for validation, business and generic errors.

## 9) Production hardening roadmap
- Outbox pattern for reliable event publication.
- Redis cache for tenant resolution and catalogs.
- Kafka/RabbitMQ adapter layer.
- OpenTelemetry traces and metrics.
- Secrets in Vault/KMS and JWT key rotation with key ids.
