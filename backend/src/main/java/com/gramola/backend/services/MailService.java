package com.gramola.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender; // Cliente SMTP de Spring para el envío de correos

    /**
     * Envía un correo electrónico de texto plano usando el remitente configurado.
     */
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage(); // Construcción del mensaje de texto plano
        
        message.setFrom("no-reply@gramola.com"); // Remitente por defecto
        message.setTo(to); // Dirección del destinatario
        message.setSubject(subject); // Asunto del correo
        message.setText(text); // Cuerpo del mensaje
        
        mailSender.send(message); // Ejecuta el envío físico a través de SMTP
    }
}
