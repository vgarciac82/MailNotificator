# Manual Técnico: Microservicio de Evidencias de Notificación

Este documento sirve como guía para configurar, ejecutar y consumir el microservicio `AutomatedNotification`.

## 1. Requisitos del Entorno
*   **Java:** JDK 21
*   **Construcción:** Maven Wrapper incluido (`mvnw`)
*   **Puertos:** 8080 (por defecto, modificable en `application.properties`)

## 2. Compilación y Ejecución
Para compilar el proyecto y descargar dependencias:
```bash
./mvnw clean package -DskipTests
```

Para arrancar el microservicio:
```bash
./mvnw spring-boot:run
```

## 3. Configuración SMTP Dinámica (Hot-Swap)
El sistema utiliza **Spring Data JPA** con una base de datos en memoria (H2) para gestionar de forma dinámica el servidor de salida de correos.
*   **Consola BD:** Disponible en `http://localhost:8080/h2-console`
*   **JDBC URL:** `jdbc:h2:mem:mailnotificator` (Usuario: `sa`, sin contraseña)

Para cambiar el servidor SMTP en tiempo de ejecución (sin reiniciar la aplicación), basta con ejecutar un SQL UPDATE:
```sql
UPDATE smtp_configuration SET active = false;
INSERT INTO smtp_configuration (host, port, username, password, protocol, active, auth_enabled, start_tls_enabled)
VALUES ('nuevo.host.com', 587, 'usuario', 'pass', 'smtp', true, true, true);
```
El sistema siempre leerá el registro marcado con `active = true` antes de enviar.

## 4. Seguridad (API Key)
Todo el tráfico entrante al prefijo `/api/v1/` está protegido por un interceptor.
Debes enviar la cabecera HTTP:
`X-API-KEY: AxtelSecr3tK3y2026!`
*(Este valor se configura en `application.properties` en la variable `app.security.api-key`)*.

## 5. Especificación de la API (Consumo)

### Enviar Notificación
**Endpoint:** `POST /api/v1/notifications/send`
**Headers Requeridos:**
*   `Content-Type: application/json`
*   `X-API-KEY: <tu_api_key>`

**Cuerpo de Petición (JSON):**
```json
{
  "to": "proveedor@dominio.com",
  "from": "institucion@gob.mx",
  "subject": "Título del Correo",
  "body": "<h1>Contenido</h1>",
  "procedureFolio": "ADJ-2026-01",
  "supplierId": "PRV-001"
}
```

**Respuesta Exitosa (HTTP 200):**
```json
{
  "notificationUuid": "d5337e01-4680-4eb1-b98f-72cd4733d1a4",
  "base64Eml": "RGF0ZTogTW9...",
  "emlHash": "8637c88a0799e57539a7a65d01123885ebc2283bba132ec04b1d1b46b9dd9e8e",
  "messageId": "<93a950ec-8fe2-45e3-8881-ffaea7470b85@axtel.automated.notification>",
  "queueId": "39DCD1A800CD",
  "syncStatus": "ACCEPTED_BY_RELAY",
  "smtpResponseText": "250 2.0.0 Ok: queued as 39DCD1A800CD\n"
}
```

## 6. Integración con Sistema Legacy
El sistema cliente (Legacy) debe consumir este endpoint. Es absoluta responsabilidad del Legacy:
1. Atrapar la respuesta JSON.
2. Decodificar o almacenar de forma segura la cadena `base64Eml`.
3. Validar el estado `syncStatus` (que debe ser `ACCEPTED_BY_RELAY` para considerarse encolado con éxito).
4. Actualizar su propia base de datos (Bitácora Principal) utilizando el `notificationUuid` devuelto.
