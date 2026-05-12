package com.axtel.automated.notification.controller;

import com.axtel.automated.notification.dto.SendEmailRequest;
import com.axtel.automated.notification.dto.SendEmailResponse;
import com.axtel.automated.notification.service.EmailNotificationEvidencerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class EmailNotificationController {

    private final EmailNotificationEvidencerService emailService;

    @Autowired
    public EmailNotificationController(EmailNotificationEvidencerService emailService) {
        this.emailService = emailService;
    }

    /**
     * Endpoint para enviar una invitación/notificación de forma auditable.
     * Requiere el header X-API-KEY.
     */
    @PostMapping("/send")
    public ResponseEntity<SendEmailResponse> sendNotification(@Valid @RequestBody SendEmailRequest request) {
        SendEmailResponse response = emailService.sendEmailWithEvidence(request);
        return ResponseEntity.ok(response);
    }
}
