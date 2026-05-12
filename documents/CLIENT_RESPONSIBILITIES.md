# Responsabilidades del Cliente (Sistema Legacy)

Dado que el módulo de evidencia de notificaciones por correo electrónico opera como un servicio puro y agnóstico de infraestructura de datos, el sistema que consume este servicio (el "cliente" o sistema legacy) asume responsabilidades críticas para garantizar la persistencia, auditabilidad y manejo de errores de las invitaciones.

## 1. Persistencia y Bitácora Principal

El cliente debe crear y gestionar una tabla principal en su base de datos consolidada por invitación/proveedor. Esta tabla servirá como la bitácora oficial y auditable del envío.
Cada registro debe contener al menos:
- `id` (Llave primaria)
- `procedure_folio` (Folio del procedimiento)
- `supplier_id` (Identificador del proveedor)
- `destination_email` (Correo destino)
- `subject` (Asunto)
- `created_at` (Fecha de creación del registro)
- `send_attempt_at` (Fecha del intento de envío)
- `sent_by_user` (Usuario que detonó la acción)
- `notification_uuid` (UUID único, generado por el servicio de correos)
- `message_id` (Identificador técnico del mensaje MIME)
- `queue_id` (Identificador del mensaje en la cola de Postfix, obtenido de forma síncrona si es posible)
- `eml_hash` (Hash del archivo EML para garantizar su integridad)
- `application_status` (Estado dentro de la aplicación cliente)
- `smtp_status` (Estado inmediato devuelto por el servicio, ej. ACCEPTED_BY_RELAY)
- `smtp_response_text` (Respuesta textual del servidor SMTP)

## 2. Almacenamiento del Archivo EML (Base64)

El servicio de envío devolverá la copia exacta del correo emitido (con todas sus cabeceras y cuerpo) en formato `.eml`, codificado en **Base64**.
Es responsabilidad del cliente:
- Decodificar el Base64 de forma segura si requiere inspeccionar, parsear o guardar el archivo crudo en el *file system* local.
- Almacenar este string Base64 (o su versión decodificada binaria) en un lugar seguro (por ejemplo: NAS, Amazon S3, o como campo de tipo BLOB/CLOB en la base de datos).
- El sistema cliente debe prever qué esquema de codificación de caracteres utilizar al manejar este string (se recomienda fuertemente UTF-8) para no corromper la evidencia criptográfica ni alterar el `eml_hash`.

## 3. Tabla de Eventos Técnicos (Opcional en Etapa 1)

Si en el futuro se implementa un procesamiento de logs asíncronos (ver `FUTURE_FUNCTIONALITIES.md`), el cliente deberá tener una tabla hija para guardar cada evento técnico en el tiempo.
Campos sugeridos:
- `id`
- `notification_uuid`
- `event_timestamp`
- `queue_id`
- `mapped_internal_status`
- `smtp_response_text`

## 4. Transaccionalidad, Estado y Reintentos

El servicio de correo provisto es **stateless** (sin estado propio). La durabilidad y el manejo de flujos alternativos recae enteramente en el cliente:
- Si el servicio arroja una excepción de conexión o falla (ej. timeout SMTP), el cliente debe atraparla y decidir, según la lógica de negocio, si hace un *rollback* completo de la base de datos o si registra el intento como fallido y encola la tarea para un reintento manual o automático.
- Si la respuesta del servicio (`SendEmailResponse`) indica que el correo fue rechazado síncronamente (`REJECTED`), el cliente debe registrar este rechazo en su base de datos y prevenir falsos positivos de "correo enviado" en la interfaz de usuario.

## 5. Scripts SQL de Referencia

A modo de sugerencia, el equipo encargado de la base de datos del sistema legacy deberá generar:
1. DDL para tabla principal de notificaciones (`quotation_email_notification`).
2. DDL para tabla de eventos técnicos (`quotation_email_notification_event`).
3. Índices útiles y optimizados para correlación rápida, priorizando búsquedas por:
   - `procedure_folio`, `supplier_id`, `destination_email`
   - `notification_uuid`, `message_id`, `queue_id`
