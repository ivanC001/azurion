# Plantillas de WhatsApp en Azurion

## Alcance

Se completo el flujo existente de la bandeja. No se crearon endpoints paralelos ni se guardan tokens en el navegador.

- Backend: `WhatsappIntegrationService`, `WhatsappCloudApiClient`, DTO de plantillas y mensajes, entidad `CrmWhatsappMessage`.
- Nuevos componentes internos: `WhatsappTemplate` (variables, validacion y renderizado) y `WhatsappTemplateParser` (adaptacion de Meta).
- Frontend: `whatsapp-inbox-page`, tipos CRM y `whatsapp-template.utils`.
- Migracion tenant: `V96__whatsapp_message_template_snapshot.sql`, incluida en el plan de migraciones CRM.

## API existente

Rutas relativas al contexto HTTP del backend:

- `GET /v1/saas/crm/whatsapp/plantillas`
- `POST /v1/saas/crm/prospectos/{prospectoId}/whatsapp/plantillas`
- El alias `/crm` y los endpoints de mensajes, conversaciones y webhook se conservan.

El GET agrega `id`, `estado`, `disponible`, `motivoNoDisponible` y `componentes` a los campos existentes.
El POST conserva `{ nombre, idioma, parametros: string[] }`. Los parametros siguen el orden de los componentes devueltos: HEADER y luego BODY. En cada componente, las variables numericas se ordenan por indice; las variables con nombre, por primera aparicion. Una variable repetida necesita un solo valor por componente.

## Sincronizacion y aislamiento

Se consulta `/{WABA_ID}/message_templates` con el token del canal del tenant actual. Se recorren las paginas de Meta mediante el cursor `after`, sin seguir URLs externas de paginacion con credenciales.

El boton de sincronizacion siempre vuelve a Meta. No hay una cache compartida ni persistencia de catalogos entre tenants: la bandeja mantiene su lista mientras esta abierta y no consulta las plantillas en su sondeo de mensajes. Antes de cada envio el backend vuelve a consultar Meta para validar nombre, idioma y aprobacion actuales. Se evito una cache adicional porque la consulta solo ocurre al entrar, sincronizar y enviar; no se mantiene una transaccion de base de datos durante esta consulta.

Solo se listan plantillas APPROVED. Las que necesitan medios, URL dinamica u otros componentes no soportados se muestran deshabilitadas. Se admiten variables de texto posicionales o con nombre en HEADER/BODY, pie estatico y botones estaticos. El frontend recibe textos y variables, no ejemplos de Meta ni credenciales.

## Variables y envio

No se busca un nombre de plantilla especifico. El compositor reconoce nombres de variable o el contexto del texto (por ejemplo `Hola {{1}}` y `solicitud sobre {{2}}`) para sugerir nombre e interes. Las variables desconocidas quedan vacias; todas son editables. La vista previa conserva saltos de linea y no sustituye recursivamente el contenido de un parametro.

El backend valida prospecto, conversacion existente, telefono, canal activo, credenciales, plantilla/idioma, compatibilidad, cantidad y contenido de parametros. No acepta parametros vacios, marcadores literales, controles ni mas de 1024 caracteres por valor. Meta sigue siendo la autoridad final sobre sus restricciones y disponibilidad.

El envio usa `/{PHONE_NUMBER_ID}/messages`, `type=template`, nombre, idioma y componentes validados. Para variables con nombre se incluye `parameter_name`. No se reintenta automaticamente un POST de envio para evitar duplicados ante una respuesta incierta.

Cuando Meta devuelve wamid se registra el mensaje saliente y su asesor en la entidad existente. Se guarda el texto renderizado y una instantanea de nombre, idioma y parametros (componente/variable/valor). El frontend muestra `Plantilla`, nombre e idioma y agrega el mensaje solamente al chat desde el que se envio.

## Datos y despliegue

V96 agrega columnas opcionales a `crm_whatsapp_messages`:

- `plantilla_nombre VARCHAR(512)`
- `plantilla_idioma VARCHAR(35)`
- `plantilla_parametros_json TEXT`

Los mensajes anteriores siguen funcionando; no se inventan datos de plantilla para el historial antiguo. Los webhooks pueden actualizar `raw_payload` y estados sin borrar la instantanea. Los estados retrasados no hacen retroceder LEIDO a ENVIADO. Los fallos de entrega guardan codigo y detalle de Meta.

Desplegar backend y frontend juntos y ejecutar la migracion tenant V96 mediante el mecanismo de migracion del proyecto antes de usar los nuevos campos. No se requieren nuevas claves de entorno.

## Ventana de atencion

Enviar una plantilla no modifica `ultimo_entrante_en`. El texto libre y los PDF requieren una respuesta del cliente dentro de las ultimas 24 horas. Sin mensaje entrante, la ventana permanece cerrada. Solo el webhook de un mensaje del cliente actualiza la fecha; un evento entrante antiguo no la hace retroceder.

## Prueba manual con seguimiento_prospecto

1. Confirmar en WhatsApp Manager que `seguimiento_prospecto`, idioma `es_PE`, esta APPROVED en la WABA configurada para la empresa. Si sigue PENDING, no aparecera disponible.
2. Confirmar token vigente con acceso a la WABA/numero, canal activo, Phone Number ID, webhook y permisos del asesor.
3. Abrir una conversacion existente cuya ventana este cerrada. El contacto debe tener telefono internacional valido, nombre e interes.
4. Sincronizar y seleccionar `seguimiento_prospecto · es_PE`.
5. Revisar el nombre y el interes sugeridos. Editarlos y comprobar la vista previa. Vaciar Interes debe deshabilitar Enviar y mostrar `{{2}}` como faltante.
6. Con un destinatario de prueba autorizado, enviar una sola vez. Confirmar wamid, texto, asesor, etiqueta de plantilla y posterior estado de entrega. Aceptacion de Meta no equivale a entrega al destinatario.
7. Confirmar que el texto libre sigue cerrado. Responder desde el telefono del cliente y verificar que el webhook habilita una nueva ventana.
8. Probar otra empresa: la lista y el envio deben usar su propia configuracion. Revocar/pausar una plantilla en Meta y comprobar que no puede enviarse tras actualizar.

Las pruebas automatizadas usan un servidor HTTP local controlado, no envian mensajes reales ni simulan entregas dentro de la aplicacion. La confirmacion real requiere las credenciales vigentes del tenant y un destinatario autorizado.

Referencia: [coleccion oficial de Meta Cloud API](https://www.postman.com/meta/whatsapp-business-platform/documentation/wlk6lh4/whatsapp-cloud-api).
