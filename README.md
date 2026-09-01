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

Cada tenant nuevo recibe una cuenta administrativa inicial activa:

- usuario: `admin`
- contrasena temporal: `admin1`
- rol: `ADMIN_EMPRESA`

El administrador general o el administrador del tenant debe cambiar esta contrasena
desde la gestion de usuarios antes de entregar el acceso definitivo.

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

## Reenganche por WhatsApp (fuera de la ventana de 24 horas)

Meta cierra la conversacion 24 horas despues del ultimo mensaje del cliente. Pasado
ese plazo solo acepta **plantillas aprobadas**, asi que para retomar el contacto una
semana despues hay que programar el envio de una plantilla.

El CRM lo hace con una cola (`public.crm_whatsapp_reengagement_outbox`) que un worker
sondea y envia cuando llega la fecha. Las condiciones se vuelven a evaluar al enviar,
no al programar:

- si el cliente respondio, se omite: la ventana esta abierta y el asesor puede escribir
  texto libre, que no se cobra por plantilla;
- si el prospecto pidio la baja, se omite. La baja se detecta sola en los mensajes
  entrantes ("STOP", "baja", "no me escriban"...) y se puede marcar a mano;
- fuera del horario comercial del tenant se reprograma a la proxima franja habil.

### Como configurarlo

`GET /api/v1/saas/crm/whatsapp/reenganches/guia` responde que falta para poder
programar: revisa el catalogo real del WABA del tenant, avisa si una plantilla
aprobada no se puede enviar y por que, y devuelve un modelo de plantilla listo para
copiar en el Administrador de WhatsApp. Es el primer lugar a mirar cuando "no aparece
mi plantilla".

Reglas que conviene tener presentes al crear la plantilla en Meta:

- solo texto (cuerpo obligatorio, encabezado y pie opcionales). Un encabezado de
  imagen o un boton de URL con variables la vuelven no enviable desde el CRM;
- variables numeradas en orden (`{{1}}`, `{{2}}`, ...); el pie no admite variables;
- botones de respuesta rapida: cuando el cliente toca uno se reabre la ventana de
  24 horas;
- las plantillas de ejemplo de Meta, como `hello_world`, solo salen desde sus numeros
  de prueba.

Sobre la categoria: **Utility** entrega mejor y cuesta menos, pero solo califica si el
mensaje habla de algo concreto que el cliente pidio (una cotizacion con su numero,
monto y vencimiento). Un "¿sigues interesado?" es reenganche generico y Meta lo
clasifica como **Marketing**, que tambien funciona pero se factura mas caro y queda
sujeto a sus limites de entrega.

### Endpoints

| Metodo | Ruta (bajo `/api/v1/saas/crm`) | Que hace |
|---|---|---|
| GET | `/whatsapp/reenganches/guia` | Que falta para poder programar |
| POST | `/prospectos/{id}/whatsapp/reenganches` | Programa un envio |
| POST | `/prospectos/{id}/whatsapp/reenganches/cotizacion` | Programa citando una cotizacion y llena las variables solo |
| GET | `/prospectos/{id}/whatsapp/reenganches` | Lista los del prospecto |
| DELETE | `/prospectos/{id}/whatsapp/reenganches` | Cancela los pendientes |
| POST | `/prospectos/{id}/whatsapp/baja` | Registra el opt-out |
| DELETE | `/prospectos/{id}/whatsapp/baja` | Revierte el opt-out |

Variables relevantes:

- `CRM_REENGAGEMENT_ENABLED` (default `true`)
- `CRM_REENGAGEMENT_HOUR_START` / `CRM_REENGAGEMENT_HOUR_END` (default `9` / `20`)
- `CRM_REENGAGEMENT_DAYS` (default `1,2,3,4,5,6`, de lunes a sabado)
- `CRM_REENGAGEMENT_POLL_DELAY_MS` (default `60000`)
