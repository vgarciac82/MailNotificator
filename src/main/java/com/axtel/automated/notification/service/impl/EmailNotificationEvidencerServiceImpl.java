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

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailNotificationEvidencerServiceImpl implements EmailNotificationEvidencerService {

    private final SmtpConfigurationRepository smtpRepository;

    @Autowired
    public EmailNotificationEvidencerServiceImpl(SmtpConfigurationRepository smtpRepository) {
        this.smtpRepository = smtpRepository;
    }

    @Override
    public SendEmailResponse sendEmailWithEvidence(SendEmailRequest request) {
        // 1. Obtener config en caliente
        SmtpConfiguration config = smtpRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new EmailNotificationException("No hay configuracion SMTP activa en base de datos."));

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
            if (request.getAttachmentsBase64() != null) {
                for (Map.Entry<String, String> entry : request.getAttachmentsBase64().entrySet()) {
                    byte[] attachmentBytes = Base64.getDecoder().decode(entry.getValue());
                    helper.addAttachment(entry.getKey(), new jakarta.mail.util.ByteArrayDataSource(attachmentBytes, "application/octet-stream"));
                }
            }

            // Aplicar cambios al MIME para que recalcule headers
            message.saveChanges();

            // 4. Generar Evidencia Cruda (Base64 y Hash) antes de enviar
            byte[] emlBytes = EmlUtils.extractEmlBytes(message);
            response.setBase64Eml(Base64.getEncoder().encodeToString(emlBytes));
            response.setEmlHash(EmlUtils.calculateSha256(emlBytes));

            // 5. Enviar usando capa baja SMTPTransport para extraer la respuesta 250 OK
            response.setSyncStatus(EmailNotificationStatus.SENT_TO_RELAY);
            
            try (SMTPTransport transport = (SMTPTransport) session.getTransport(config.getProtocol() != null ? config.getProtocol() : "smtp")) {
                if (Boolean.TRUE.equals(config.getAuthEnabled())) {
                    transport.connect(config.getHost(), config.getPort(), config.getUsername(), config.getPassword());
                } else {
                    transport.connect(config.getHost(), config.getPort(), null, null);
                }
                
                transport.sendMessage(message, message.getAllRecipients());
                
                // 6. Extraer respuesta SMTP
                String serverResponse = transport.getLastServerResponse();
                response.setSmtpResponseText(serverResponse);
                
                if (serverResponse != null && serverResponse.startsWith("250")) {
                    response.setSyncStatus(EmailNotificationStatus.ACCEPTED_BY_RELAY);
                    response.setQueueId(extractQueueId(serverResponse));
                } else {
                    response.setSyncStatus(EmailNotificationStatus.REJECTED);
                }
            }

            return response;

        } catch (MessagingException | IOException e) {
            // Error en la comunicacion SMTP o armado del MIME
            response.setSyncStatus(EmailNotificationStatus.REJECTED);
            response.setSmtpResponseText(e.getMessage());
            throw new EmailNotificationException("Error en el envio de correo: " + e.getMessage(), e);
        }
    }

    /**
     * Intenta extraer el Queue ID de la respuesta tipica de Postfix.
     * Ejemplo de Postfix: "250 2.0.0 Ok: queued as 3A1B2C4D5E"
     */
    private String extractQueueId(String response) {
        if (response == null) return null;
        // Regex comun para Postfix
        Pattern pattern = Pattern.compile("queued as ([A-Za-z0-9]+)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
