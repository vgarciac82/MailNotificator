# AutomatedNotification (Módulo de Evidencia de Correo)

Microservicio desacoplado basado en Spring Boot, encargado de generar evidencia técnica auditable y criptográfica de los correos electrónicos institucionales enviados a proveedores.

## 🚀 Características Principales

1. **Evidencia Inmutable**: Genera y retorna el contenido crudo del correo (`.eml`) codificado en Base64 junto a su hash criptográfico SHA-256.
2. **Auditoría Técnica**: Extrae síncronamente el `queue_id` de la capa de transporte SMTP (ej. Postfix) interceptando la respuesta `250 OK`.
3. **Hot-Swap de SMTP**: Permite cambiar el servidor de correo activo en milisegundos a través de una actualización a la base de datos (H2/JPA), ideal para mitigar bloqueos de IP (Blacklists) o saturaciones.
4. **Seguridad**: Protegido contra abusos internos (IDOR / Spam) mediante autenticación por `X-API-KEY`.

## 🛠️ Stack Tecnológico

- **Java**: 21
- **Framework**: Spring Boot 3.x
- **Mensajería**: Jakarta Mail (Eclipse Angus)
- **Construcción**: Maven
- **Base de Datos**: H2 (en memoria) + Spring Data JPA

## 📚 Documentación del Proyecto

El análisis arquitectónico, manual de integración y decisiones técnicas se encuentran en el directorio `/documents`.

- [Arquitectura (Componentes y Secuencia)](./documents/ARCHITECTURE.md)
- [Manual Técnico e Integración de API](./documents/TECHNICAL_MANUAL.md)
- [Definición de Requisitos y Contratos (DTO)](./documents/DEFINITION.md)
- [Responsabilidades del Cliente Legacy](./documents/CLIENT_RESPONSIBILITIES.md)
- [Funcionalidades Futuras (Etapa 2)](./documents/FUTURE_FUNCTIONALITIES.md)

## 🏃‍♀️ Arranque Rápido (Quick Start)

### 1. Levantar el servicio
```bash
./mvnw spring-boot:run
```
El microservicio arrancará por defecto en el puerto `8080`.

### 2. Probar envío de correo
```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: AxtelSecr3tK3y2026!" \
  -d '{
    "to": "test@example.com",
    "from": "institucion@gob.mx",
    "subject": "Correo de Prueba Auditable",
    "body": "<h1>Hola</h1><p>Prueba técnica</p>",
    "procedureFolio": "ADJ-2026-TEST"
  }'
```

---
*Desarrollado para la auditoría de procedimientos de adjudicación directa menores a 300 UMAS.*
