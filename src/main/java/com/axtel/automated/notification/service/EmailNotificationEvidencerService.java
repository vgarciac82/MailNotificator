package com.axtel.automated.notification.service;

import com.axtel.automated.notification.dto.SendEmailRequest;
import com.axtel.automated.notification.dto.SendEmailResponse;

public interface EmailNotificationEvidencerService {
    
    /**
     * Construye y envía un correo electrónico extrayendo evidencias técnicas (Base64, Hash y Queue ID).
     * 
     * @param request Datos del correo a enviar.
     * @return Evidencia síncrona del envío.
     */
    SendEmailResponse sendEmailWithEvidence(SendEmailRequest request);
}
