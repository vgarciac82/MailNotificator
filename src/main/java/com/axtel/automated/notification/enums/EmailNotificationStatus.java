package com.axtel.automated.notification.enums;

/**
 * Representa los estados internos (síncronos) de una notificación de correo enviada.
 */
public enum EmailNotificationStatus {
    GENERATED,
    SENT_TO_RELAY,
    ACCEPTED_BY_RELAY,
    REJECTED
}
