package com.example.gramolaRodrigo.security;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para simplificar la práctica
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (Login, Registro, Confirmación)
                .requestMatchers("/bars/login", "/bars/register", "/bars/confirm-email", "/bars/reset-password", "/bars/forgot-password", "/bars/resend-verification", "/bars/subscription-status").permitAll()
                
                // Rutas de Pagos y Planes (NECESARIAS PARA SUSCRIPCIÓN)
                .requestMatchers("/plans/**", "/payments/**", "/plans/song-price").permitAll()
                
                // Rutas de la Gramola (Cola, búsqueda, streaming)
                .requestMatchers("/queue/**", "/songs/**", "/api/deezer/**").permitAll()
                
                // Cualquier otra cosa requiere autenticación (o permítelo todo si prefieres no bloquear nada)
                .anyRequest().permitAll() 
            );
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir el origen de Angular
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}