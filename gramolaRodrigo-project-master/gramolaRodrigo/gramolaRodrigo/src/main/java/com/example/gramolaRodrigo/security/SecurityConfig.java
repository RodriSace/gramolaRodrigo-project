package com.example.gramolaRodrigo.security;

import java.util.List;

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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. RUTAS PÚBLICAS Y DE SISTEMA
                .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/assets/**", "/login", "/static/**").permitAll()
                
                // 2. RUTAS DE BAR Y AUTH
                .requestMatchers("/bars/register", "/bars/login", "/bars/confirm-email/**", "/bars/subscription-status").permitAll()
                
                // 3. NUEVAS RUTAS DESBLOQUEADAS (Solución al 403 de /queue y /plans)
                .requestMatchers("/queue/**", "/plans/**", "/songs/**").permitAll()
                .requestMatchers("/api/deezer/**", "/payments/**").permitAll()
                
                // El resto requiere autenticación (si decides usar roles más adelante)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .logout(logout -> logout.permitAll());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Usamos patrones para permitir el puerto de Angular
        config.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Access-Control-Allow-Origin"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}