package com.example.gramolaRodrigo.controllers;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.controllers.dto.RegisterRequest;
import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.services.BarService;

@RestController
@RequestMapping("/bars")
// IMPORTANTE: Se ha eliminado @CrossOrigin aquí para que no choque con SecurityConfig
public class BarController {

    private final BarService barService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BarController.class);

    public BarController(BarService barService) {
        this.barService = barService;
    }

    /**
     * Endpoint para registrar un nuevo bar.
     * Permite el acceso público desde SecurityConfig.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> registerBar(@RequestBody RegisterRequest registrationRequest) {
        LOGGER.info(">>> PETICIÓN DE REGISTRO RECIBIDA PARA: {}", registrationRequest.getName());
        try {
            if (registrationRequest.getPwd1() == null || registrationRequest.getPwd2() == null ||
                !registrationRequest.getPwd1().equals(registrationRequest.getPwd2())) {
                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
            }

            barService.registerBar(
                registrationRequest.getName(),
                registrationRequest.getEmail(),
                registrationRequest.getPwd1(),
                registrationRequest.getClientId(),
                registrationRequest.getClientSecret()
            );
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        } catch (IllegalStateException | DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            LOGGER.error("Error crítico registrando bar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/confirm-email")
    public ResponseEntity<?> confirmEmail(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Token requerido");
        }
        
        Optional<Bar> barOpt = barService.confirmEmailAndGetBar(token);
        if (barOpt.isPresent()) {
            return ResponseEntity.ok(barOpt.get());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token inválido o expirado");
    }

    @GetMapping("/confirm-email/status")
    public ResponseEntity<?> confirmEmailStatus(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) return ResponseEntity.badRequest().build();
        Map<String, Object> status = barService.getEmailVerificationStatus(token);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email es requerido");
        }
        try {
            barService.resendEmailVerification(email);
            return ResponseEntity.ok("Enlace enviado");
        } catch (Exception e) {
            LOGGER.error("Error reenviando verificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint de autenticación (Login).
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginBar(@RequestBody com.example.gramolaRodrigo.controllers.dto.LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getPwd() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Campos obligatorios faltantes");
        }

        Optional<Bar> barOptional = barService.loginBar(loginRequest.getEmail(), loginRequest.getPwd());

        if (barOptional.isPresent()) {
            LOGGER.info("Login exitoso para: {}", loginRequest.getEmail());
            return ResponseEntity.ok(barOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email o contraseña incorrectos.");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email es requerido");
        }
        try {
            barService.createPasswordResetTokenForEmail(email);
            return ResponseEntity.ok("Proceso iniciado");
        } catch (Exception e) {
            LOGGER.error("Error en forgot-password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        
        if (token == null || newPassword == null || token.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token y contraseña requeridos");
        }

        boolean ok = barService.resetPasswordWithToken(token, newPassword);
        if (ok) {
            return ResponseEntity.ok("Contraseña actualizada");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token inválido");
        }
    }

    @GetMapping("/subscription-status")
    public ResponseEntity<?> subscriptionStatus(@RequestParam("barId") String barId) {
        if (barId == null || barId.isBlank()) return ResponseEntity.badRequest().build();
        boolean active = barService.hasActiveSubscription(barId);
        return ResponseEntity.ok(Map.of("active", active));
    }
}