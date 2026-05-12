Actúa como un desarrollador senior Java 21 especializado en microservicios y bibliotecas con Spring Boot y envío de correo moderno (Jakarta Mail). Debes implementar la ETAPA 1 de un módulo de evidencia auditable de envío de invitaciones por correo electrónico para procedimientos de adjudicación directa menores a 300 UMAS.

## Contexto funcional

En este proceso, la institución debe invitar por correo electrónico al menos a tres proveedores para solicitar cotizaciones. No se requiere probar lectura del destinatario ni respuesta del proveedor. El objetivo es demostrar ante auditoría que el sistema NO simula envíos y que realmente generó, emitió y recibió una confirmación síncrona por parte del servidor SMTP de cada invitación.

La evidencia síncrona requerida en esta etapa debe permitir demostrar, por cada proveedor invitado:

1. que el sistema generó el correo,
2. qué contenido tenía (archivo `.eml`),
3. cuándo se emitió,
4. a qué destinatario fue dirigido,
5. qué identificadores técnicos se asociaron (`Message-ID`, `Queue-ID` y `UUID`),
6. y cuál fue el resultado técnico *inmediato* (ej. aceptación por el relay Postfix).

## Decisión arquitectónica de esta etapa

El módulo operará como una **biblioteca / servicio desacoplado**, puro y agnóstico de base de datos, usando Spring Boot.
La responsabilidad de persistir (escribir en tablas de BD), controlar las transacciones y reintentos, y guardar el archivo `.eml` será **absolutamente del sistema cliente (Legacy u otros microservicios)**. 

Para consultar el esquema de base de datos sugerido y responsabilidades del cliente, consulta `CLIENT_RESPONSIBILITIES.md`.
Para consultar características postergadas (parseo de logs asíncronos), consulta `FUTURE_FUNCTIONALITIES.md`.

## Objetivo técnico

Implementar un servicio sólido y mantenible que haga lo siguiente:

1. construir el correo de invitación (MIME vía Jakarta Mail).
2. generar internamente un identificador técnico único (`notificationUuid`) por envío.
3. generar una copia exacta del correo en formato `.eml` y devolverla codificada en **Base64**.
4. enviar el mensaje por SMTP relay.
5. capturar la respuesta síncrona de Postfix para extraer el `queue_id`.
6. devolver un objeto de respuesta (`SendEmailResponse`) con toda la evidencia del envío, dejando al sistema cliente la responsabilidad de decidir cómo y dónde almacenarlo.

## Requisitos funcionales y técnicos obligatorios

### A. Identificadores técnicos

Debes manejar y distinguir claramente estos identificadores en la capa de servicio:

- `notificationUuid`: UUID **generado por el servicio** para cada invitación.
- `messageId`: encabezado `Message-ID` del correo, generado e inyectado explícitamente por el servicio.
- `queueId`: identificador interno asignado por Postfix. El servicio debe extraerlo síncronamente del texto de respuesta SMTP `250 OK`.

### B. Headers de correlación

Al construir el mensaje, agregar estos encabezados personalizados:

- `X-SAI-Procedure-Folio`
- `X-SAI-Supplier-Id`
- `X-SAI-Notification-UUID`

### C. Almacenamiento y Evidencia del Correo Emitido

Generar una copia exacta del correo emitido, incluyendo headers, body y adjuntos.
El servicio debe:
1. Calcular y devolver un hash (ej. SHA-256) del contenido `.eml` para evidencia de integridad.
2. Devolver el contenido del `.eml` codificado en **Base64** dentro de la respuesta, dejando la responsabilidad de almacenamiento seguro al cliente.

### D. Interfaz del Servicio y DTOs

Definir los siguientes contratos/POJOs:

**`SendEmailRequest` (Input)**:
- `to`
- `from`
- `subject`
- `body`
- `responseTo` (reply-to)
- `attachments` (ej. mapa de nombres de archivo y `byte[]`)
- `procedureFolio` (para logs y cabeceras)
- `supplierId` (para logs y cabeceras)

**`SendEmailResponse` (Output - Evidencia)**:
- `notificationUuid` (generado por el servicio)
- `base64Eml` (contenido exacto del archivo EML codificado)
- `emlHash` (SHA-256)
- `messageId`
- `queueId` (extraído si fue exitoso)
- `syncStatus` (Estado síncrono del Enum)
- `smtpResponseText` (texto crudo de la respuesta SMTP del relay)

### E. Estados internos (Síncronos)

Definir un enum de estados para reflejar el envío síncrono:
- `GENERATED`
- `SENT_TO_RELAY`
- `ACCEPTED_BY_RELAY`
- `REJECTED`

### F. Integración propuesta

El flujo de uso de la aplicación que consumirá tu servicio será:
1. El cliente instancia y puebla un `SendEmailRequest`.
2. Inyecta y llama a tu componente de Spring: `emailNotificationEvidencerService.send(...)`.
3. Tu servicio arma el MIME, asigna UUID, inyecta `Message-ID`, calcula el Base64 y Hash, se conecta al SMTP, envía, intercepta `queue_id` desde el `250 OK`, y retorna `SendEmailResponse`.
4. El sistema cliente atrapa el objeto de respuesta, decodifica si es necesario, y efectúa sus transacciones JDBC hacia sus tablas de bitácora, manejando errores o reintentos si hubo fallas.

### G. Entregables esperados

Devuelve:

1. Proyecto inicializado con Spring Boot y Java 21 (Maven).
2. Clases DTO de Java (`SendEmailRequest`, `SendEmailResponse`).
3. Enum `EmailNotificationStatus`.
4. Implementación del `EmailNotificationEvidencerService` (anotado como `@Service` de Spring) que interactúa con Jakarta Mail/SMTP y extrae el `queue_id`.
5. Clases de Utilidades para generación de MIME a Base64 y encriptación de Hash (`EmlUtils` o similar).
6. Ejemplo de controlador REST (`@RestController`) o fachada que ilustre cómo se invocaría este servicio.
7. Manejo claro de excepciones (por ejemplo, timeout, error de autenticación, fallo de parseo MIME) con respuesta tipificada, para que el cliente atrape un error y marque un status `REJECTED` u otro pertinente.

### H. Restricciones finales

NO implementar:
- Tablas SQL, Repositorios JPA o DAOs.
- Parseo asíncrono de logs de Postfix (postergado para etapa 2).
- Batch o Importadores programados.
- Funcionalidad IMAP/POP para leer correos rebotados.

Empieza inicializando el proyecto Maven de Spring Boot con Java 21, y posteriormente las clases.