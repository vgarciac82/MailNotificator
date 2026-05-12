package com.axtel.automated.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class SendEmailRequest {

    @NotBlank
    @Email
    private String to;

    @NotBlank
    @Email
    private String from;

    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    @Email
    private String responseTo;

    // Mapa de nombre de archivo -> contenido en Base64
    private Map<String, String> attachmentsBase64;

    private String procedureFolio;
    private String supplierId;

    public SendEmailRequest() {
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getResponseTo() {
        return responseTo;
    }

    public void setResponseTo(String responseTo) {
        this.responseTo = responseTo;
    }

    public Map<String, String> getAttachmentsBase64() {
        return attachmentsBase64;
    }

    public void setAttachmentsBase64(Map<String, String> attachmentsBase64) {
        this.attachmentsBase64 = attachmentsBase64;
    }

    public String getProcedureFolio() {
        return procedureFolio;
    }

    public void setProcedureFolio(String procedureFolio) {
        this.procedureFolio = procedureFolio;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }
}
