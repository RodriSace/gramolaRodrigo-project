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
public class BarController {

    private final BarService barService;
    private static final Logger LOGGER = LoggerFactory.getLogger(BarController.class);

    public BarController(BarService barService) {
        this.barService = barService;
    }

    // Endpoint para registrar un nuevo bar
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
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (DataIntegrityViolationException e) {
            // En caso de condiciones de carrera que violen la unicidad
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            LOGGER.error("Error registrando bar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Confirmación de email mediante token
    @GetMapping("/confirm-email")
    public ResponseEntity<?> confirmEmail(@RequestParam("token") String token) {
        // Intentamos confirmar y devolver el Bar (incluye caso idempotente si ya estaba verificado)
        Optional<Bar> barOpt = barService.confirmEmailAndGetBar(token);
        if (barOpt.isPresent()) {
            return ResponseEntity.ok(barOpt.get());
        }
        // Si falla, damos un mensaje genérico (frontend puede consultar /confirm-email/status para más detalle)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token inválido o expirado");
    }

    // Estado del token de confirmación (para depurar y mostrar mensajes más claros)
    @GetMapping("/confirm-email/status")
    public ResponseEntity<?> confirmEmailStatus(@RequestParam("token") String token) {
        Map<String, Object> status = barService.getEmailVerificationStatus(token);
        return ResponseEntity.ok(status);
    }

    // Reenviar email de verificación generando un token nuevo (si ya está verificado también respondemos OK)
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email es requerido");
        }
        try {
            barService.resendEmailVerification(email);
            return ResponseEntity.ok("Si existe una cuenta no verificada, se ha enviado un nuevo enlace (ver logs)");
        } catch (Exception e) {
            LOGGER.error("Error reenviando verificación", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno");
        }
    }

    // Endpoint para hacer login
    @PostMapping("/login")
    public ResponseEntity<?> loginBar(@RequestBody com.example.gramolaRodrigo.controllers.dto.LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getPwd() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email y contraseña son requeridos.");
        }
        Optional<Bar> barOptional = barService.loginBar(loginRequest.getEmail(), loginRequest.getPwd());

        if (barOptional.isPresent()) {
            // Si el login es exitoso, devolvemos el objeto Bar y un 200 OK
            return ResponseEntity.ok(barOptional.get());
        } else {
            // Si el login falla, devolvemos un error 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email o contraseña incorrectos.");
        }
    }

    // Endpoint: solicitar recuperación de contraseña (se genera un token y se muestra en logs)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email es requerido");
        }

        try {
            java.util.Optional<String> maybeToken = barService.createPasswordResetTokenForEmail(email);
            if (maybeToken.isPresent()) {
                String token = maybeToken.get();
                // Aquí enviaríamos el email; por ahora lo dejamos registrado
                LOGGER.info("Password reset token para {} -> {}", email, token);
                return ResponseEntity.ok("Token generado y enviado (ver logs en el servidor)");
            } else {
                // No revelamos si el email existe por seguridad, devolvemos OK
                return ResponseEntity.ok("Token generado y enviado (ver logs en el servidor)");
            }
        } catch (Exception e) {
            LOGGER.error("Error en forgot-password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno");
        }
    }

    // Endpoint: resetear contraseña usando token
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (token == null || token.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token y nueva contraseña son requeridos");
        }

        boolean ok = barService.resetPasswordWithToken(token, newPassword);
        if (ok) {
            return ResponseEntity.ok("Contraseña actualizada");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token inválido o expirado");
        }
    }

    // Estado de suscripción de un bar
    @GetMapping("/subscription-status")
    public ResponseEntity<?> subscriptionStatus(@RequestParam("barId") String barId) {
        boolean active = barService.hasActiveSubscription(barId);
        return ResponseEntity.ok(java.util.Map.of("active", active));
    }
}