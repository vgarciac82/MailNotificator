package com.axtel.automated.notification.service.impl;

import com.axtel.automated.notification.dto.SendEmailRequest;
import com.axtel.automated.notification.dto.SendEmailResponse;
import com.axtel.automated.notification.entity.SmtpConfiguration;
import com.axtel.automated.notification.enums.EmailNotificationStatus;
import com.axtel.automated.notification.exception.EmailNotificationException;
import com.axtel.automated.notification.repository.SmtpConfigurationRepository;
import com.axtel.automated.notification.service.EmailNotificationEvidencerService;
import com.axtel.automated.notification.utils.EmlUtils;
import org.eclipse.angus.mail.smtp.SMTPTransport;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailNotificationEvidencerServiceImpl implements EmailNotificationEvidencerService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationEvidencerServiceImpl.class);

    private final SmtpConfigurationRepository smtpRepository;

    @Autowired
    public EmailNotificationEvidencerServiceImpl(SmtpConfigurationRepository smtpRepository) {
        this.smtpRepository = smtpRepository;
    }

    @Override
    public SendEmailResponse sendEmailWithEvidence(SendEmailRequest request) {
        log.debug("Iniciando flujo de envío de correo. Destinatario: {}, Remitente: {}, Asunto: {}", request.getTo(), request.getFrom(), request.getSubject());
        
        // 1. Obtener config en caliente
        SmtpConfiguration config = smtpRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new EmailNotificationException("No hay configuracion SMTP activa en base de datos."));

        log.info("Enviando correo usando el servidor SMTP: {}:{} (Protocolo: {}, Auth: {}, STARTTLS: {})", 
                config.getHost(), config.getPort(), config.getProtocol(), config.getAuthEnabled(), config.getStartTlsEnabled());

        String notificationUuid = UUID.randomUUID().toString();
        SendEmailResponse response = new SendEmailResponse();
        response.setNotificationUuid(notificationUuid);
        response.setSyncStatus(EmailNotificationStatus.GENERATED);

        // 2. Preparar sesión JavaMail dinámica
        Properties props = new Properties();
        props.put("mail.transport.protocol", config.getProtocol() != null ? config.getProtocol() : "smtp");
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", String.valueOf(config.getPort()));
        
        if (Boolean.TRUE.equals(config.getAuthEnabled())) {
            props.put("mail.smtp.auth", "true");
        }
        if (Boolean.TRUE.equals(config.getStartTlsEnabled())) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        Session session = Session.getInstance(props);
        
        try {
            // 3. Construir mensaje MIME
            log.debug("Construyendo mensaje MIME para el UUID: {}", notificationUuid);
            MimeMessage message = new MimeMessage(session);
            
            // Header: Message-ID controlado por nosotros (opcional pero util para correlacion)
            String generatedMessageId = "<" + UUID.randomUUID().toString() + "@axtel.automated.notification>";
            message.addHeader("Message-ID", generatedMessageId);
            response.setMessageId(generatedMessageId);
            
            // Headers institucionales obligatorios
            message.addHeader("X-SAI-Notification-UUID", notificationUuid);
            if (request.getProcedureFolio() != null) {
                message.addHeader("X-SAI-Procedure-Folio", request.getProcedureFolio());
            }
            if (request.getSupplierId() != null) {
                message.addHeader("X-SAI-Supplier-Id", request.getSupplierId());
            }

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(request.getTo());
            helper.setFrom(request.getFrom());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true); // Asumiendo HTML por defecto
            
            if (request.getResponseTo() != null && !request.getResponseTo().isEmpty()) {
                helper.setReplyTo(request.getResponseTo());
            }

            // Procesar adjuntos en Base64
            if (request.getAttachmentsBase64() != null && !request.getAttachmentsBase64().isEmpty()) {
                log.debug("Procesando {} archivo(s) adjunto(s)", request.getAttachmentsBase64().size());
                for (Map.Entry<String, String> entry : request.getAttachmentsBase64().entrySet()) {
                    byte[] attachmentBytes = Base64.getDecoder().decode(entry.getValue());
                    helper.addAttachment(entry.getKey(), new jakarta.mail.util.ByteArrayDataSource(attachmentBytes, "application/octet-stream"));
                }
            }

            // Aplicar cambios al MIME para que recalcule headers
            message.saveChanges();

            // 4. Generar Evidencia Cruda (Base64 y Hash) antes de enviar
            log.debug("Generando evidencia digital del correo (.eml)");
            byte[] emlBytes = EmlUtils.extractEmlBytes(message);
            response.setBase64Eml(Base64.getEncoder().encodeToString(emlBytes));
            response.setEmlHash(EmlUtils.calculateSha256(emlBytes));

            // 5. Enviar usando capa baja SMTPTransport para extraer la respuesta 250 OK
            response.setSyncStatus(EmailNotificationStatus.SENT_TO_RELAY);
            
            String protocol = config.getProtocol() != null ? config.getProtocol() : "smtp";
            log.debug("Abriendo transporte JavaMail para protocolo: {}", protocol);
            try (SMTPTransport transport = (SMTPTransport) session.getTransport(protocol)) {
                if (Boolean.TRUE.equals(config.getAuthEnabled())) {
                    log.debug("Intentando conectar a SMTP {}:{} con usuario '{}'", config.getHost(), config.getPort(), config.getUsername());
                    transport.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
                } else {
                    log.debug("Intentando conectar a SMTP {}:{} sin autenticación", config.getHost(), config.getPort());
                    transport.connect(config.getHost(), config.getPort(), null, null);
                }
                
                log.debug("Conexión SMTP exitosa. Enviando mensaje...");
                transport.sendMessage(message, message.getAllRecipients());
                
                // 6. Extraer respuesta SMTP
                String serverResponse = transport.getLastServerResponse();
                response.setSmtpResponseText(serverResponse);
                log.info("Mensaje enviado. Respuesta del servidor SMTP: '{}'", serverResponse != null ? serverResponse.trim() : "null");
                
                if (serverResponse != null && serverResponse.startsWith("250")) {
                    response.setSyncStatus(EmailNotificationStatus.ACCEPTED_BY_RELAY);
                    response.setQueueId(extractQueueId(serverResponse));
                    log.info("Envío confirmado exitosamente. Queue ID asignada: {}", response.getQueueId());
                } else {
                    response.setSyncStatus(EmailNotificationStatus.REJECTED);
                    log.warn("El servidor SMTP aceptó la conexión pero no retornó 250 OK. Estado asignado: REJECTED");
                }
            }

            return response;

        } catch (MessagingException | IOException e) {
            // Error en la comunicacion SMTP o armado del MIME
            response.setSyncStatus(EmailNotificationStatus.REJECTED);
            response.setSmtpResponseText(e.getMessage());
            log.error("Fallo crítico en el flujo de envío de correo SMTP: {}", e.getMessage(), e);
            throw new EmailNotificationException("Error en el envio de correo: " + e.getMessage(), e);
        }
    }

    /**
     * Intenta extraer el Queue ID de la respuesta tipica de Postfix.
     * Ejemplo de Postfix: "250 2.0.0 Ok: queued as 3A1B2C4D5E"
     */
    private String extractQueueId(String response) {
        if (response == null) return null;
        
        // 1. Intentar formato Postfix: "queued as XXXXX"
        Pattern postfixPattern = Pattern.compile("queued as ([A-Za-z0-9]+)");
        Matcher postfixMatcher = postfixPattern.matcher(response);
        if (postfixMatcher.find()) {
            return postfixMatcher.group(1);
        }
        
        // 2. Intentar formato Exchange/Otros: "<ID> Queued mail for delivery"
        Pattern bracketPattern = Pattern.compile("<([^>]+)>");
        Matcher bracketMatcher = bracketPattern.matcher(response);
        if (bracketMatcher.find()) {
            return bracketMatcher.group(1);
        }
        
        return null;
    }
}
