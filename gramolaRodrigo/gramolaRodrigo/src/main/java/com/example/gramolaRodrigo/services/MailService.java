package com.example.gramolaRodrigo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    public void send(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        try {
            mailSender.send(msg);
            LOGGER.info("Email enviado a {} con asunto '{}'", to, subject);
        } catch (Exception e) {
            // No rompemos el flujo si no hay SMTP configurado: lo dejamos en logs con detalle
            LOGGER.error("No se pudo enviar email a {}: {}", to, e.getMessage(), e);
            LOGGER.info("Contenido del email simulado -> TO: {} | SUBJECT: {} | BODY: {}", to, subject, text);
        }
    }
}
