package com.axtel.automated.notification.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EmlUtils {

    private EmlUtils() {
        // Utility class
    }

    /**
     * Extrae el contenido completo de un MimeMessage como un arreglo de bytes.
     */
    public static byte[] extractEmlBytes(MimeMessage message) throws IOException, MessagingException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            message.writeTo(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Devuelve el MimeMessage codificado en Base64.
     */
    public static String toBase64(MimeMessage message) throws IOException, MessagingException {
        byte[] bytes = extractEmlBytes(message);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Calcula el hash SHA-256 de los bytes dados y lo devuelve en formato Hexadecimal.
     */
    public static String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error calculando SHA-256", e);
        }
    }
}
