package com.example.gramolaRodrigo.services;

import java.time.Instant; // Importa la clase Optional
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.repositories.BarRepository;

@Service
public class BarService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BarService.class);

    // Inyección por constructor (soluciona la advertencia amarilla)
    private final BarRepository barRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.gramolaRodrigo.repositories.PasswordResetTokenRepository tokenRepository;
    private final com.example.gramolaRodrigo.repositories.EmailVerificationTokenRepository emailTokenRepository;
    private final com.example.gramolaRodrigo.repositories.SubscriptionRepository subscriptionRepository;
    private final MailService mailService;
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public BarService(BarRepository barRepository, PasswordEncoder passwordEncoder,
            com.example.gramolaRodrigo.repositories.PasswordResetTokenRepository tokenRepository,
            com.example.gramolaRodrigo.repositories.EmailVerificationTokenRepository emailTokenRepository,
            com.example.gramolaRodrigo.repositories.SubscriptionRepository subscriptionRepository,
            MailService mailService) {
        this.barRepository = barRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mailService = mailService;
    }

    public Bar registerBar(String name, String email, String pwd, String clientId, String clientSecret) {
        // Validaciones básicas
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (pwd == null || pwd.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        // En desarrollo permitimos registrar sin credenciales de Deezer (se podrán añadir después)

        // Escenarios alternativos: si existe un bar previo no usable, lo eliminamos y seguimos
        barRepository.findByEmail(email).ifPresent(existing -> {
            boolean hasActiveSubscription = subscriptionRepository.findAll().stream()
                .anyMatch(s -> email.equals(barRepository.findById(s.getBarId()).map(Bar::getEmail).orElse(null))
                        && "ACTIVE".equalsIgnoreCase(s.getStatus())
                        && (s.getEndAt() == null || s.getEndAt().isAfter(java.time.Instant.now())));
            if (!existing.isVerified() || !hasActiveSubscription) {
                // Borramos tokens asociados y suscripciones no críticas
                emailTokenRepository.findAll().stream()
                    .filter(t -> existing.getId().equals(t.getBarId()))
                    .forEach(emailTokenRepository::delete);
                tokenRepository.findAll().stream()
                    .filter(t -> existing.getId().equals(t.getBarId()))
                    .forEach(tokenRepository::delete);
                subscriptionRepository.findAll().stream()
                    .filter(s -> existing.getId().equals(s.getBarId()))
                    .forEach(subscriptionRepository::delete);
                barRepository.delete(existing);
            } else {
                throw new IllegalStateException("El email ya está registrado y activo");
            }
        });
        if (barRepository.existsByName(name)) {
            throw new IllegalStateException("El nombre de bar ya existe");
        }

        // Creamos una nueva instancia de la entidad Bar
        Bar newBar = new Bar();
        
        // Asignamos un ID único y aleatorio
        newBar.setId(UUID.randomUUID().toString());
        
        // Asignamos los datos recibidos
        newBar.setName(name);
    newBar.setEmail(email);
        
        // Encriptamos la contraseña antes de guardarla
        newBar.setPwd(passwordEncoder.encode(pwd));

    // Guardamos credenciales del bar si se proporcionan (opcionales en dev)
    if (clientId != null && !clientId.trim().isEmpty()) newBar.setClientId(clientId);
    if (clientSecret != null && !clientSecret.trim().isEmpty()) newBar.setClientSecret(clientSecret);

        // Guardamos el bar
        Bar saved = barRepository.save(newBar);

        // Generamos token de verificación de email (30 min)
    String token = createEmailVerificationForBar(saved);
    String link = frontendBaseUrl + "/confirm-email?token=" + token;
    LOGGER.info("URL de confirmación generada (modo práctica): {}", link);
    try {
        mailService.send(saved.getEmail(), "Confirma tu email", "Bienvenido a Gramola Virtual!\n\nConfirma tu email haciendo clic: " + link);
    } catch (Exception ignore) {
        // En entorno de prácticas podemos simular el envío (no romper el registro por SMTP)
    }

        return saved;
    }

    public Optional<Bar> loginBar(String email, String pwd) {
        // 1. Buscamos el bar por su email usando el repositorio
        Optional<Bar> optionalBar = barRepository.findByEmail(email);

        // 2. Verificamos si el bar existe
        if (optionalBar.isPresent()) {
            Bar bar = optionalBar.get();
            // 3. Comparamos la contraseña proporcionada con la encriptada en la BD
            if (pwd != null && passwordEncoder.matches(pwd, bar.getPwd())) {
                // Si coinciden, devolvemos el bar encontrado
                return optionalBar;
            }
        }
        
        // 4. Si el bar no existe o la contraseña es incorrecta, devolvemos un Optional vacío
        return Optional.empty();
    }

    // Comprueba si el bar tiene una suscripción activa (status ACTIVE y endAt futuro o null)
    public boolean hasActiveSubscription(String barId) {
        java.time.Instant now = java.time.Instant.now();
        return subscriptionRepository.findAll().stream()
            .anyMatch(s -> barId.equals(s.getBarId()) && "ACTIVE".equalsIgnoreCase(s.getStatus())
                && (s.getEndAt() == null || s.getEndAt().isAfter(now)));
    }

    // Crear token de verificación de email y guardarlo
    public String createEmailVerificationForBar(Bar bar) {
        // Eliminamos tokens previos del mismo bar para evitar confusión
        emailTokenRepository.findAll().stream()
            .filter(t -> bar.getId().equals(t.getBarId()))
            .forEach(emailTokenRepository::delete);

        String token = UUID.randomUUID().toString();
        com.example.gramolaRodrigo.entities.EmailVerificationToken evt = new com.example.gramolaRodrigo.entities.EmailVerificationToken();
        evt.setToken(token);
        evt.setBarId(bar.getId());
        evt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        evt.setUsed(false);
        emailTokenRepository.save(evt);
        return token;
    }

    // Confirmar email con token
    public boolean confirmEmail(String token) {
        Optional<com.example.gramolaRodrigo.entities.EmailVerificationToken> opt = emailTokenRepository.findByToken(token);
        if (opt.isEmpty()) {
            LOGGER.warn("Confirmación fallida: token no encontrado");
            return false;
        }
        com.example.gramolaRodrigo.entities.EmailVerificationToken evt = opt.get();
        if (evt.getExpiresAt().isBefore(Instant.now())) {
            emailTokenRepository.delete(evt);
            LOGGER.warn("Confirmación fallida: token expirado");
            return false;
        }
        Optional<Bar> barOpt = barRepository.findById(evt.getBarId());
        if (barOpt.isEmpty()) {
            LOGGER.warn("Confirmación fallida: bar asociado no encontrado");
            return false;
        }
        Bar bar = barOpt.get();
        if (evt.isUsed()) {
            // Idempotente: si ya estaba usado, consideramos confirmación válida sin cambiar nada
            LOGGER.info("Token ya usado previamente; estado verificado actual: {}", bar.isVerified());
            return bar.isVerified();
        }
        bar.setVerified(true);
        bar.setVerifiedAt(Instant.now());
        barRepository.save(bar);
        evt.setUsed(true);
        evt.setUsedAt(Instant.now());
        emailTokenRepository.save(evt);
        return true;
    }

    // Igual que confirmEmail pero devolviendo el Bar cuando hay éxito (incluye caso ya usado/idempotente)
    public Optional<Bar> confirmEmailAndGetBar(String token) {
        Optional<com.example.gramolaRodrigo.entities.EmailVerificationToken> opt = emailTokenRepository.findByToken(token);
        if (opt.isEmpty()) {
            LOGGER.warn("Confirmación fallida: token no encontrado");
            return Optional.empty();
        }
        com.example.gramolaRodrigo.entities.EmailVerificationToken evt = opt.get();
        if (evt.getExpiresAt().isBefore(Instant.now())) {
            emailTokenRepository.delete(evt);
            LOGGER.warn("Confirmación fallida: token expirado");
            return Optional.empty();
        }
        Optional<Bar> barOpt = barRepository.findById(evt.getBarId());
        if (barOpt.isEmpty()) {
            LOGGER.warn("Confirmación fallida: bar asociado no encontrado");
            return Optional.empty();
        }
        Bar bar = barOpt.get();
        if (evt.isUsed()) {
            LOGGER.info("Token ya usado previamente; devolviendo bar verificado={} ", bar.isVerified());
            return bar.isVerified() ? Optional.of(bar) : Optional.empty();
        }
        bar.setVerified(true);
        bar.setVerifiedAt(Instant.now());
        barRepository.save(bar);
        evt.setUsed(true);
        evt.setUsedAt(Instant.now());
        emailTokenRepository.save(evt);
        return Optional.of(bar);
    }

    // Devuelve un mapa con estado del token para diagnóstico en frontend
    public java.util.Map<String, Object> getEmailVerificationStatus(String token) {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        Optional<com.example.gramolaRodrigo.entities.EmailVerificationToken> opt = emailTokenRepository.findByToken(token);
        if (opt.isEmpty()) {
            res.put("status", "not-found");
            return res;
        }
        com.example.gramolaRodrigo.entities.EmailVerificationToken evt = opt.get();
        res.put("status", "found");
        res.put("used", evt.isUsed());
        res.put("expiresAt", evt.getExpiresAt());
        boolean expired = evt.getExpiresAt().isBefore(Instant.now());
        res.put("expired", expired);
        Optional<Bar> barOpt = barRepository.findById(evt.getBarId());
        res.put("barFound", barOpt.isPresent());
        barOpt.ifPresent(b -> res.put("barVerified", b.isVerified()));
        return res;
    }

    // Reenvía (regenera) un token de verificación si el bar no está verificado; si ya está verificado, no hace nada
    public void resendEmailVerification(String email) {
        Optional<Bar> barOpt = barRepository.findByEmail(email);
        if (barOpt.isEmpty()) return; // No revelamos información
        Bar bar = barOpt.get();
        if (bar.isVerified()) return; // Ya verificado, nada que hacer
        String token = createEmailVerificationForBar(bar);
        String link = frontendBaseUrl + "/confirm-email?token=" + token;
        LOGGER.info("URL de confirmación re-generada (modo práctica): {}", link);
        try {
            mailService.send(bar.getEmail(), "Confirma tu email", "Por favor confirma tu email haciendo clic: " + link);
        } catch (Exception ignore) {}
    }

    // Genera y guarda un token de recuperación asociado a un bar (por email)
    public Optional<String> createPasswordResetTokenForEmail(String email) {
        Optional<Bar> optionalBar = barRepository.findByEmail(email);
        if (optionalBar.isEmpty()) return Optional.empty();

        Bar bar = optionalBar.get();
        String token = UUID.randomUUID().toString();

        com.example.gramolaRodrigo.entities.PasswordResetToken prt = new com.example.gramolaRodrigo.entities.PasswordResetToken();
        prt.setToken(token);
        prt.setBarId(bar.getId());
        prt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));

        tokenRepository.save(prt);
        String link = frontendBaseUrl + "/reset-password?token=" + token;
        LOGGER.info("URL de recuperación generada (modo práctica): {}", link);
        try {
            mailService.send(bar.getEmail(), "Recupera tu contraseña", "Para cambiar tu contraseña, usa este enlace: " + link);
        } catch (Exception ignore) {}
        return Optional.of(token);
    }

    // Resetea la contraseña si el token existe y no ha expirado
    public boolean resetPasswordWithToken(String token, String newPassword) {
        Optional<com.example.gramolaRodrigo.entities.PasswordResetToken> t = tokenRepository.findByToken(token);
        if (t.isEmpty()) return false;
        com.example.gramolaRodrigo.entities.PasswordResetToken prt = t.get();
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(prt);
            return false;
        }

        Optional<Bar> barOpt = barRepository.findById(prt.getBarId());
        if (barOpt.isEmpty()) return false;

        Bar bar = barOpt.get();
        bar.setPwd(passwordEncoder.encode(newPassword));
        barRepository.save(bar);

        // Consumir token
        tokenRepository.delete(prt);
        return true;
    }
}