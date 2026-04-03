package com.packed_go.auth_service.services;

public interface EmailService {
    
    /**
     * Envía un email de verificación al usuario
     * @param email Email del destinatario
     * @param username Nombre de usuario
     * @param verificationToken Token de verificación
     */
    void sendVerificationEmail(String email, String username, String verificationToken);
    
    /**
     * Envía un email de recuperación de contraseña
     * @param email Email del destinatario
     * @param username Nombre de usuario
     * @param resetToken Token de reset de contraseña
     */
    void sendPasswordResetEmail(String email, String username, String resetToken);
}