package com.gramola.backend.controllers;

import com.gramola.backend.models.User;
import com.gramola.backend.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://127.0.0.1:4200") 
public class UserController {

    @Autowired
    private UserService userService; // Servicio principal para la lógica de negocio de usuarios

    @Autowired
    private com.gramola.backend.repositories.SystemConfigRepository configRepository; // Acceso a la configuración del sistema (como URLs del frontend)

    // ========================================================
    // 📝 FLUJO 1: UN BARMAN SE REGISTRA — PASO 3
    // Recibe los datos de registro, compara contraseñas y llama al servicio
    // ========================================================
    /**
     * Registra un nuevo barman/usuario.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody Map<String, String> body) {
        String bar = body.get("bar");
        String email = body.get("email");
        String pwd1 = body.get("pwd1");
        String pwd2 = body.get("pwd2");
        String clientId = body.get("clientId");
        String clientSecret = body.get("clientSecret");

        // Verifica que ambas contraseñas coincidan
        if (!pwd1.equals(pwd2)) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        try {
            // Registra al usuario usando el servicio de negocio
            userService.register(bar, email, pwd1, clientId, clientSecret);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // ========================================================
    // 📧 FLUJO 2: EL BARMAN CONFIRMA SU EMAIL — PASO 2
    // Recibe la petición GET desde el enlace de correo y redirige al pago de suscripción
    // ========================================================
    /**
     * Confirma la cuenta de un usuario mediante token y redirige a la pasarela de pago.
     */
    @GetMapping("/confirmToken/{email}")
    public void confirmToken(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            boolean confirmed = userService.confirmAccount(token); // Intenta activar la cuenta
            if (confirmed) {
                // Obtiene la URL base del frontend para la redirección
                String feUrl = configRepository.findByKey("frontend_url")
                        .map(c -> c.getValue())
                        .orElse("http://127.0.0.1:4200");
                response.sendRedirect(feUrl + "/payment?token=" + token); // Redirige al pago de suscripción
            } else {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Token inválido");
            }
        } catch (Exception e) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    // ========================================================
    // 💳 FLUJO 3: EL BARMAN PAGA SU SUSCRIPCIÓN — RUTA B (Paso 4)
    // Recibe la petición desde Angular tras el pago exitoso en Stripe para activar cuenta en base de datos
    // ========================================================
    /**
     * Activa manualmente la suscripción de un usuario.
     */
    @PostMapping("/activate-subscription")
    public ResponseEntity<Map<String, String>> activate(@RequestBody Map<String, String> payload) {
        try {
            String token = payload.get("token");
            userService.activateSubscriptionByToken(token); // Fuerza la activación sin pasar por Stripe
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Suscripción activada con éxito.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY — PASO 2
    // Valida credenciales, comprueba confirmación y suscripción activa
    // ========================================================
    /**
     * Realiza el inicio de sesión del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody Map<String, String> credentials) {
        User user = userService.login(credentials.get("email"), credentials.get("password")); // Valida credenciales
        if (user != null && user.isConfirmed() && user.isSubscriptionActive()) {
            return ResponseEntity.ok(user); // Inicio correcto, usuario activo
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // ========================================================
    // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 2
    // Actualiza el estado memorizado del reproductor en base de datos al seleccionar playlist
    // ========================================================
    /**
     * Actualiza el estado de reproducción memorizado del reproductor.
     */
    @PostMapping("/update-playback")
    public ResponseEntity<Void> updatePlayback(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String uri = (String) payload.get("uri");
        Integer index = (Integer) payload.get("index");
        userService.updatePlaybackState(userId, uri, index); // Almacena el último estado de Spotify
        return ResponseEntity.ok().build();
    }

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA"
    // ========================================================

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 2
    // Recibe el email y llama al servicio para generar el UUID temporal de recuperación
    // ========================================================
    /**
     * Procesa la solicitud de recuperación de contraseña.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> payload) {
        try {
            userService.generateResetToken(payload.get("email")); // Envía correo con token temporal
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Se ha enviado un correo con el token de recuperación.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 3
    // Recibe el clic del email en el backend, valida el token y redirige a la vista de cambio de clave en Angular
    // ========================================================
    /**
     * Redirige al usuario tras pulsar el enlace de recuperación de contraseña.
     */
    @GetMapping("/resetTokenRedirect/{email}")
    public void resetTokenRedirect(@PathVariable String email, @RequestParam String token, HttpServletResponse response) throws IOException {
        try {
            boolean valid = userService.isResetTokenValid(token); // Comprueba validez del token de restablecimiento
            if (valid) {
                // Obtiene la URL base del frontend para redirigir
                String feUrl = configRepository.findByKey("frontend_url")
                        .map(c -> c.getValue())
                        .orElse("http://127.0.0.1:4200");
                response.sendRedirect(feUrl + "/reset-password?token=" + token); // Envía al formulario de nueva clave
            } else {
                response.sendError(HttpStatus.BAD_REQUEST.value(), "Token inválido o expirado");
            }
        } catch (Exception e) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 4
    // Recibe la nueva contraseña, la actualiza en MySQL y destruye el token de recuperación para que sea de un solo uso
    // ========================================================
    /**
     * Restablece la contraseña utilizando el token de recuperación.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> payload) {
        try {
            userService.resetPasswordWithToken(payload.get("token"), payload.get("newPassword")); // Cambia la clave en BD
            Map<String, String> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Contraseña actualizada correctamente.");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
