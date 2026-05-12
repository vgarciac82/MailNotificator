package com.axtel.automated.notification.dto;

import com.axtel.automated.notification.enums.EmailNotificationStatus;

public class SendEmailResponse {

    private String notificationUuid;
    private String base64Eml;
    private String emlHash;
    private String messageId;
    private String queueId;
    private EmailNotificationStatus syncStatus;
    private String smtpResponseText;

    public SendEmailResponse() {
    }

    public String getNotificationUuid() {
        return notificationUuid;
    }

    public void setNotificationUuid(String notificationUuid) {
        this.notificationUuid = notificationUuid;
    }

    public String getBase64Eml() {
        return base64Eml;
    }

    public void setBase64Eml(String base64Eml) {
        this.base64Eml = base64Eml;
    }

    public String getEmlHash() {
        return emlHash;
    }

    public void setEmlHash(String emlHash) {
        this.emlHash = emlHash;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public EmailNotificationStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(EmailNotificationStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getSmtpResponseText() {
        return smtpResponseText;
    }

    public void setSmtpResponseText(String smtpResponseText) {
        this.smtpResponseText = smtpResponseText;
    }
}
