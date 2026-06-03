package com.gramola.backend.services;

import com.gramola.backend.models.User;
import com.gramola.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository; // Acceso a los datos de usuario de la BD

    @Autowired
    private com.gramola.backend.repositories.SystemConfigRepository configRepository; // Acceso a la configuración global de la base de datos

    @Autowired
    private MailService mailService; // Envío de notificaciones por email

    // ========================================================
    // 📝 FLUJO 1: UN BARMAN SE REGISTRA — PASO 4 y 5
    // Comprueba existencia del email, borra cuentas fantasma si aplica, crea el usuario y envía email con UUID
    // ========================================================
    /**
     * Registra un nuevo barman/local, generando un token de confirmación y enviando
     * el correo de verificación.
     */
    public void register(String barName, String email, String password, String clientId, String clientSecret)
            throws Exception {
        System.out.println("Iniciando registro para: " + email);

        // Busca en MySQL si ya existe alguien con ese email
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            User u = existingUser.get();
            System.out.println("Usuario existente encontrado. Confirmado: " + u.isConfirmed() + ", Suscripción: "
                    + u.isSubscriptionActive());

            // Si existe pero nunca confirmó el email o nunca pagó -> borra esa cuenta fantasma de MySQL. Esto cumple el Requisito 2.2.
            if (!u.isConfirmed() || !u.isSubscriptionActive()) {
                System.out.println("Borrando cuenta antigua no confirmada/pagada.");
                userRepository.delete(u);
                userRepository.flush();
            } else {
                throw new Exception("El bar ya está registrado y activo.");
            }
        }

        // Creación del perfil del nuevo barman
        User user = new User();
        user.setBarName(barName);
        user.setEmail(email);
        
        // Guarda la contraseña
        user.setPassword(password);
        
        user.setSpotifyClientId(clientId);
        user.setSpotifyClientSecret(clientSecret);
        
        // Genera un UUID aleatorio (código secreto tipo "a3f5-b2c1...") y lo guarda como token de confirmación
        user.setConfirmationToken(UUID.randomUUID().toString());
        
        // Pone el precio de canción a 100 céntimos (1€) por defecto
        user.setSongPriceCents(100L);

        // Guarda todo en MySQL con userRepository.save()
        userRepository.save(user);
        System.out.println("Usuario guardado. Enviando correo a Mailtrap...");

        // Envío de correo de confirmación
        try {
            // Lee la URL del backend de MySQL (tabla system_config), NO está hardcodeada
            String backendUrl = configRepository.findByKey("backend_url")
                    .map(c -> c.getValue())
                    .orElse("http://127.0.0.1:8080");

            String confirmUrl = backendUrl + "/users/confirmToken/" + email + "?token=" + user.getConfirmationToken();

            // Envía el email por SMTP a Mailtrap con el enlace que contiene el token UUID
            // apuntando al backend
            mailService.sendEmail(email, "Confirma tu Gramola",
                    "¡Bienvenido a Gramola Virtual! Haz clic aquí para activar tu bar: " + confirmUrl);
            System.out.println("Correo enviado a " + email + ". URL de confirmación: " + confirmUrl);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo de confirmación: " + e.getMessage());
        }

        System.out.println("Registro completado con éxito para: " + email);
    }

    // ========================================================
    // 📧 FLUJO 2: EL BARMAN CONFIRMA SU EMAIL — PASO 3
    // Busca en MySQL al usuario por token y pone su estado confirmed a true
    // ========================================================
    /**
     * Confirma la cuenta de un usuario mediante su token.
     */
    public boolean confirmAccount(String token) {
        return userRepository.findByConfirmationToken(token)
                .map(user -> {
                    user.setConfirmed(true);
                    userRepository.save(user);
                    return true;
                }).orElse(false);
    }

    // ========================================================
    // 💳 FLUJO 3: EL BARMAN PAGA SU SUSCRIPCIÓN — RUTA B (Paso 4)
    // Activa la suscripción poniendo el campo subscriptionActive a true en MySQL utilizando el token
    // ========================================================
    /**
     * Activa la suscripción de un usuario tras procesar el pago.
     */
    public void activateSubscriptionByToken(String token) {
        User user = userRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RuntimeException("Token no encontrado"));
        user.setSubscriptionActive(true);
        userRepository.save(user);
    }

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY — PASO 2
    // Busca el usuario por email y verifica la contraseña
    // ========================================================
    /**
     * Comprueba las credenciales de email y contraseña del usuario.
     */
    public User login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword().equals(password))
                .orElse(null);
    }

    // ========================================================
    // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 2
    // Almacena la última playlist y el índice activos en MySQL
    // ========================================================
    /**
     * Actualiza el estado de reproducción del usuario (playlist y canción activa).
     */
    public void updatePlaybackState(Long userId, String uri, Integer index) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setCurrentPlaylistUri(uri);
            u.setLastPlaylistIndex(index);
            userRepository.save(u);
        });
    }

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA"
    // ========================================================

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 2
    // Busca al usuario por email, genera un token UUID temporal y le envía el email por SMTP a Mailtrap
    // ========================================================
    /**
     * Genera un token de recuperación de contraseña y envía el correo
     * correspondiente.
     */
    public void generateResetToken(String email) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("No existe una cuenta registrada con este correo."));

        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        userRepository.save(user);

        try {
            // Lee la URL del backend de MySQL (tabla system_config), NO está hardcodeada
            String backendUrl = configRepository.findByKey("backend_url")
                    .map(c -> c.getValue())
                    .orElse("http://127.0.0.1:8080");

            String resetUrl = backendUrl + "/users/resetTokenRedirect/" + email + "?token=" + token;

            // Envía el email por SMTP a Mailtrap con el enlace que contiene el token UUID
            // apuntando al backend
            mailService.sendEmail(email, "Recuperación de contraseña - Gramola Virtual",
                    "Has solicitado recuperar tu contraseña.\n\n" +
                            "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" + resetUrl);
            System.out.println("Correo de recuperación enviado a " + email + " con link: " + resetUrl);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo de recuperación: " + e.getMessage());
            throw new Exception("Error al enviar el correo de recuperación.");
        }
    }

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 3
    // Valida si el token de recuperación recibido existe en base de datos
    // ========================================================
    /**
     * Valida si un token de recuperación de contraseña existe en la base de datos.
     */
    public boolean isResetTokenValid(String token) {
        return userRepository.findByResetPasswordToken(token).isPresent();
    }

    // ========================================================
    // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 4
    // Actualiza la contraseña del usuario en MySQL y pone el resetPasswordToken a null para destruirlo
    // ========================================================
    /**
     * Actualiza la contraseña del usuario utilizando el token de recuperación y lo
     * anula.
     */
    public void resetPasswordWithToken(String token, String newPassword) throws Exception {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new Exception("Token inválido o expirado."));
        user.setPassword(newPassword);
        user.setResetPasswordToken(null);
        userRepository.save(user);
    }
}
