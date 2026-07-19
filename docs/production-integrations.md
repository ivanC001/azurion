# Envios e integraciones en produccion

Esta lista cubre los puntos que cambian entre local y produccion para leads, archivos, correo, WhatsApp y facturador.

## Nginx

El proxy debe conservar la IP real, aceptar multipart y dar tiempo suficiente al backend. El facturador se procesa de forma asincrona; no se debe mantener una peticion abierta esperando SUNAT.

Hay un bloque instalable en `deploy/nginx/azurion-locations.conf.example`. Las rutas `/api/` y `/facturador-api/` deben aparecer antes de `location /`; de lo contrario Angular responde `index.html` a las llamadas del facturador.

```nginx
location /api/ {
    client_max_body_size 10m;
    proxy_request_buffering off;
    proxy_connect_timeout 10s;
    proxy_send_timeout 30s;
    proxy_read_timeout 60s;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass http://azurion-api:8080/api/;
}

location /facturador-api/ {
    client_max_body_size 10m;
    proxy_request_buffering off;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass http://facturador:8000/api/;
}
```

Configura `RATE_LIMIT_TRUSTED_PROXIES` con la IP que el backend observa como `remoteAddr` para Nginx. No agregues redes publicas ni uses `*`: solo los proxies controlados pueden aportar `X-Forwarded-For`.

## Variables del backend

```env
CORS_ALLOWED_ORIGINS=https://azurion.tech,https://www.azurion.tech,https://tu-landing.example
RATE_LIMIT_TRUSTED_PROXIES=127.0.0.1,::1

AZURION_PRIVATE_FILES_DIR=/app/data/private-files
AZURION_PUBLIC_FILES_DIR=/app/data/public-files
MULTIPART_MAX_FILE_SIZE=8MB
MULTIPART_MAX_REQUEST_SIZE=10MB

AZURION_EMAIL_SECRET_KEY=CLAVE_ALEATORIA_DE_32_BYTES_O_MAS
SMTP_CONNECT_TIMEOUT_MS=10000
SMTP_READ_TIMEOUT_MS=20000
SMTP_WRITE_TIMEOUT_MS=20000
SMTP_TLS_PROTOCOLS=TLSv1.2 TLSv1.3
SMTP_CHECK_SERVER_IDENTITY=true

WHATSAPP_GRAPH_BASE_URL=https://graph.facebook.com
WHATSAPP_GRAPH_API_VERSION=v25.0
WHATSAPP_CONNECT_TIMEOUT_MS=5000
WHATSAPP_READ_TIMEOUT_MS=15000

FACTURADOR_BASE_URL=http://facturador:8000
FACTURADOR_API_KEY=CLAVE_COMPARTIDA
FACTURADOR_WAIT_PROCESSED_ENABLED=false
FACTURADOR_OUTBOX_ENABLED=true
FACTURADOR_CALLBACK_ENABLED=true
FACTURADOR_CALLBACK_API_KEY=CLAVE_CALLBACK
FACTURADOR_CALLBACK_SECRET=SECRETO_HMAC_CALLBACK
```

`FACTURADOR_BASE_URL=http://127.0.0.1:8000` solo funciona cuando ambos procesos comparten el mismo host. Dentro de contenedores, `127.0.0.1` apunta al propio contenedor de Azurion y no al facturador.

## Comprobaciones despues del despliegue

1. `GET /api/actuator/health` responde `UP`.
2. Los logs muestran almacenamiento privado y publico listos en rutas persistentes.
3. La prueba SMTP desde Configuracion CRM termina como `VERIFICADO`.
4. La prueba de WhatsApp valida token, WABA, Phone number ID y suscripcion.
5. Un lead de landing devuelve `200`; varios visitantes no comparten el mismo limite por IP.
6. Un pago conserva el comprobante despues de reiniciar el contenedor.
7. Una venta devuelve rapido y su estado SUNAT se actualiza por outbox/callback.

Comprueba el enrutamiento con:

```bash
curl -I https://azurion.tech/facturador-api/tenants
```

La respuesta puede ser JSON `200/401/403`, pero nunca `Content-Type: text/html`. Si devuelve HTML, Nginx esta enviando la solicitud al fallback de Angular.

No se reintentan automaticamente los POST de correo o WhatsApp: un reintento ciego puede enviar el mismo mensaje dos veces. La interfaz mantiene deshabilitado el boton hasta recibir confirmacion.
