package com.example.gramolaRodrigo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // Importante
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    @Value("${app.mail.from:no-reply@gramola.local}")
    private String from;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un email de forma asíncrona.
     * Esto evita que el registro o el login se bloqueen por fallos de red.
     */
    @Async // <--- El método ahora se ejecuta en un hilo separado
    public void send(String to, String subject, String text) {
        if (to == null || to.isEmpty()) {
            LOGGER.warn("Intento de envío de email sin destinatario");
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);

        try {
            LOGGER.info("Iniciando envío de email a {}...", to);
            mailSender.send(msg);
            LOGGER.info("Email enviado con éxito a {}", to);
        } catch (Exception e) {
            // Logueamos el error pero no lanzamos excepción para no romper el flujo del BarService
            LOGGER.error("Fallo al conectar con el servidor SMTP (Mailtrap): {}", e.getMessage());
            
            // Simulación en logs para desarrollo: permite ver el token sin necesidad de abrir Mailtrap
            LOGGER.info("--- CONTENIDO SIMULADO ---");
            LOGGER.info("Para: {}", to);
            LOGGER.info("Asunto: {}", subject);
            LOGGER.info("Cuerpo: {}", text);
            LOGGER.info("--------------------------");
        }
    }
}