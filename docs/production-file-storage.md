# Almacenamiento de archivos en produccion

Los vouchers, contratos y documentos privados del CRM se guardan fuera de la base de datos. El contenedor debe tener un volumen persistente y escribible.

## Variables obligatorias

```env
AZURION_PRIVATE_FILES_DIR=/app/data/private-files
AZURION_PUBLIC_FILES_DIR=/app/data/public-files
MULTIPART_MAX_FILE_SIZE=8MB
MULTIPART_MAX_REQUEST_SIZE=10MB
```

El `docker-compose.yml` del proyecto monta el volumen `azurion_app_data` en `/app/data`. No despliegues el backend con un filesystem de solo lectura sin montar esa ruta.

## Nginx

El proxy que publica `/api` debe aceptar el mismo tamano antes de reenviar el request a Spring:

```nginx
location /api/ {
    client_max_body_size 10m;
    proxy_request_buffering off;
    proxy_pass http://azurion-api:8080/api/;
}
```

Tras desplegar, los logs deben incluir una sola vez:

```text
CRM private file storage ready at /app/data/private-files
Almacenamiento publico listo en /app/data/public-files
```

Si la ruta no existe o no es escribible, la aplicacion falla al iniciar con un mensaje que identifica `AZURION_PRIVATE_FILES_DIR`; esto evita aceptar pagos que luego no puedan conservar su comprobante.

La configuracion completa de proxy, SMTP, WhatsApp, landings y facturador esta en `docs/production-integrations.md`.
