package com.example.gramolaRodrigo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull; // <-- 1. AÑADE ESTA IMPORTACIÓN
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) { // <-- 2. AÑADE LA ANOTACIÓN AQUÍ
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true);
    }
}