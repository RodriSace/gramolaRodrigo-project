package com.example.gramolaRodrigo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    // Deja que Spring Boot auto-configure JavaMailSender cuando haya spring.mail.*.
    // Este bean de fallback sólo se creará si Spring Boot NO crea uno (por ejemplo,
    // en tests sin propiedades). Así evitamos sobreescribir la configuración de Mailtrap.
    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    @ConditionalOnExpression("'${spring.mail.host:}' == ''")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl impl = new JavaMailSenderImpl();
        // No seteamos host/port aquí para no interferir; si se llama a send() sin
        // propiedades válidas, fallará y nuestro MailService lo capturará con logs.
        return impl;
    }
}
