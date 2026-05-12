# Funcionalidades Futuras y Estrategias Operativas (Etapa 2+)

Las siguientes capacidades han sido extraídas del alcance del MVP (Etapa 1) del módulo de evidencias, y se registran en este documento para su futura implementación o como referencia arquitectónica.

## 1. Parser de Logs de Postfix (Evidencia Asíncrona)
Implementar un parser puro y reutilizable para procesar líneas de log en texto plano generadas por el servidor de correo Postfix. Este parser debe ser capaz de extraer el `queue_id`, estatus técnico final (`sent`, `deferred`, `bounced`), código SMTP y texto de respuesta del servidor remoto, mapeándolos a estados internos.

## 2. Batch / Importador de Eventos
Implementar un proceso en lote (batch) que consuma los logs de forma recurrente, manteniendo un offset. Actualizará el estado consolidado de los correos en la bitácora principal haciendo correlaciones por `queue_id`.

## 3. Estrategias No Invasivas de Captura de Logs (Alternativas para Infraestructura Estricta)
Para evitar instalar parsers o agentes invasivos en el servidor Postfix institucional, se recomienda optar por alguna de estas arquitecturas:
*   **Patrón IMAP "Read and Delete" (Buzón de Rebotes):** Configurar una cuenta Send-Only con un buzón asignado (ej. 50 MB). Nuestro sistema se conecta periódicamente vía IMAP, extrae los `queue_id` de los NDRs (correos de error rebotados), actualiza la base de datos y ejecuta el comando `DELETE` (Expunge) sobre el correo. Esto mantiene la huella de disco en cero y evita tocar el servidor de producción.
*   **Reenvío Nativo por Syslog (Rsyslog):** Si no se aprueba el buzón, solicitar que se añada la regla `mail.* @IP_DE_NUESTRO_SERVIDOR:514` en el `rsyslog.conf` del Postfix. Esto transmite los logs por UDP/TCP en tiempo real hacia nuestra aplicación sin instalar agentes.

## 4. Estrategias Operativas ante Restricciones e Incidentes

### A. IP en Lista Negra (Blacklists de Gmail/Outlook)
Si el servidor institucional está vetado por mala reputación, nuestra arquitectura híbrida nos protege. 
*   **Solución:** Contratar un servicio SMTP transaccional externo (ej. SendGrid, Mailgun) exclusivamente para las notificaciones a proveedores. Se da de alta la credencial en nuestra tabla `smtp_configuration` y se activa el `Hot-Swap`. Los correos saldrán con IPs limpias sorteando las blacklists corporativas.

### B. Saturación del Servidor (Nóminas Masivas)
Si procesos institucionales masivos (como la nómina) saturan la cola de Postfix y entorpecen las adjudicaciones directas:
*   **Válvula Hot-Swap:** A través del registro `active=true` en la base de datos, el administrador puede desviar el tráfico de nuestro servicio hacia un Postfix de respaldo instantáneamente, sin reiniciar nuestra aplicación Spring Boot.
*   **Regulación (Throttling) desde el Cliente:** El sistema legacy que consume esta API no debe enviar ráfagas masivas. Debe implementar encolamiento interno y enviar peticiones a nuestro endpoint en lotes controlados para proteger el ancho de banda del SMTP.
*   **Defensa por Evidencia:** El registro persistido del `queue_id` síncrono deslinda a nuestro sistema de la culpa de cuellos de botella en el servidor principal, ofreciendo trazabilidad exacta de a qué hora ingresó nuestro mensaje a su cola.
