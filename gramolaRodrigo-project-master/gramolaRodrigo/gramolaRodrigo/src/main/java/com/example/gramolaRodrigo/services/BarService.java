package com.example.gramolaRodrigo.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.entities.EmailVerificationToken;
import com.example.gramolaRodrigo.entities.PasswordResetToken;
import com.example.gramolaRodrigo.repositories.BarRepository;
import com.example.gramolaRodrigo.repositories.EmailVerificationTokenRepository;
import com.example.gramolaRodrigo.repositories.PasswordResetTokenRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionRepository;

@Service
public class BarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BarService.class);

    private final BarRepository barRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MailService mailService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    // Inyección de dependencias (basado en los archivos que tienes)
    public BarService(BarRepository barRepository, 
                      PasswordEncoder passwordEncoder,
                      PasswordResetTokenRepository tokenRepository,
                      EmailVerificationTokenRepository emailTokenRepository,
                      SubscriptionRepository subscriptionRepository,
                      MailService mailService) {
        this.barRepository = barRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.mailService = mailService;
    }

    /**
     * Registra un nuevo Bar.
     * Si el email ya existe pero no está verificado ni tiene suscripción activa, lo sobrescribe (limpieza).
     */
    public Bar registerBar(String name, String email, String pwd, String clientId, String clientSecret) {
        // Validaciones básicas
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("El email es obligatorio");
        if (pwd == null || pwd.trim().isEmpty()) throw new IllegalArgumentException("La contraseña es obligatoria");

        // Lógica de limpieza de cuentas "zombies"
        barRepository.findByEmail(email).ifPresent(existing -> {
            boolean hasActiveSubscription = hasActiveSubscription(existing.getId());

            if (!existing.isVerified() || !hasActiveSubscription) {
                LOGGER.info("Limpiando cuenta no verificada o inactiva para re-registro: {}", email);
                // Borrar tokens asociados
                emailTokenRepository.findAll().stream()
                    .filter(t -> existing.getId().equals(t.getBarId()))
                    .forEach(emailTokenRepository::delete);
                tokenRepository.findAll().stream()
                    .filter(t -> existing.getId().equals(t.getBarId()))
                    .forEach(tokenRepository::delete);
                // Borrar suscripciones asociadas
                subscriptionRepository.findAll().stream()
                    .filter(s -> existing.getId().equals(s.getBar().getId())) // Ajuste según tu entidad Subscription
                    .forEach(subscriptionRepository::delete);
                
                barRepository.delete(existing);
            } else {
                throw new IllegalStateException("El email ya está registrado y activo");
            }
        });

        if (barRepository.existsByName(name)) {
            throw new IllegalStateException("El nombre de bar ya existe");
        }

        // Crear Bar
        Bar newBar = new Bar();
        newBar.setId(UUID.randomUUID().toString());
        newBar.setName(name);
        newBar.setEmail(email);
        newBar.setPwd(passwordEncoder.encode(pwd));
        
        // Credenciales opcionales (Spotify/Deezer)
        if (clientId != null && !clientId.isBlank()) newBar.setClientId(clientId);
        if (clientSecret != null && !clientSecret.isBlank()) newBar.setClientSecret(clientSecret);

        Bar saved = barRepository.save(newBar);

        // Generar Token y Enviar Email
        String token = createEmailVerificationForBar(saved);
        String link = frontendBaseUrl + "/confirm-email?token=" + token;
        
        LOGGER.info("URL confirmación (dev): {}", link);
        
        try {
            mailService.send(saved.getEmail(), "Confirma tu cuenta", "Enlace de confirmación: " + link);
        } catch (Exception e) {
            LOGGER.error("Error enviando email: {}", e.getMessage());
        }

        return saved;
    }

    public Optional<Bar> loginBar(String email, String pwd) {
        Optional<Bar> optionalBar = barRepository.findByEmail(email);
        if (optionalBar.isPresent()) {
            Bar bar = optionalBar.get();
            if (passwordEncoder.matches(pwd, bar.getPwd())) {
                return Optional.of(bar);
            }
        }
        return Optional.empty();
    }

    /**
     * Comprueba si el bar tiene una suscripción activa.
     * Utiliza el SubscriptionRepository que ya tienes.
     */
    public boolean hasActiveSubscription(String barId) {
        Instant now = Instant.now();
        // Buscamos en todas las suscripciones (idealmente harías una query JPQL, pero esto funciona con lo que tienes)
        return subscriptionRepository.findAll().stream()
            .anyMatch(s -> {
                // Verificamos relación con Bar (puede ser s.getBar().getId() o s.getBarId() según tu entidad)
                String sBarId = (s.getBar() != null) ? s.getBar().getId() : null;
                
                return barId.equals(sBarId) 
                    && "ACTIVE".equalsIgnoreCase(s.getStatus())
                    && (s.getEndAt() == null || s.getEndAt().isAfter(now));
            });
    }

    public String createEmailVerificationForBar(Bar bar) {
        // Limpieza de tokens previos
        emailTokenRepository.findAll().stream()
            .filter(t -> bar.getId().equals(t.getBarId()))
            .forEach(emailTokenRepository::delete);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setToken(token);
        evt.setBarId(bar.getId());
        evt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        evt.setUsed(false);
        emailTokenRepository.save(evt);
        return token;
    }

    public Optional<Bar> confirmEmailAndGetBar(String token) {
        Optional<EmailVerificationToken> opt = emailTokenRepository.findByToken(token);
        if (opt.isEmpty()) return Optional.empty();
        
        EmailVerificationToken evt = opt.get();
        if (evt.getExpiresAt().isBefore(Instant.now())) {
            emailTokenRepository.delete(evt);
            return Optional.empty();
        }

        Optional<Bar> barOpt = barRepository.findById(evt.getBarId());
        if (barOpt.isEmpty()) return Optional.empty();
        
        Bar bar = barOpt.get();
        
        // Si ya está usado, pero es el mismo token, lo damos por bueno (idempotencia)
        if (evt.isUsed()) {
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

    public Map<String, Object> getEmailVerificationStatus(String token) {
        Map<String, Object> res = new HashMap<>();
        Optional<EmailVerificationToken> opt = emailTokenRepository.findByToken(token);
        
        if (opt.isEmpty()) {
            res.put("status", "not-found");
            return res;
        }
        
        EmailVerificationToken evt = opt.get();
        res.put("status", "found");
        res.put("used", evt.isUsed());
        res.put("expired", evt.getExpiresAt().isBefore(Instant.now()));
        
        barRepository.findById(evt.getBarId()).ifPresent(b -> 
            res.put("barVerified", b.isVerified())
        );
        
        return res;
    }

    @SuppressWarnings("null")
    public void resendEmailVerification(String email) {
        barRepository.findByEmail(email).ifPresent(bar -> {
            if (bar.isVerified()) return;
            
            String token = createEmailVerificationForBar(bar);
            String link = frontendBaseUrl + "/confirm-email?token=" + token;
            LOGGER.info("Reenvío confirmación: {}", link);
            
            try {
                mailService.send(bar.getEmail(), "Confirma tu email", "Nuevo enlace: " + link);
            } catch (Exception e) { /* ignorar */ }
        });
    }

    public Optional<String> createPasswordResetTokenForEmail(String email) {
        Optional<Bar> barOpt = barRepository.findByEmail(email);
        if (barOpt.isEmpty()) return Optional.empty();

        Bar bar = barOpt.get();
        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setBarId(bar.getId());
        prt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        tokenRepository.save(prt);

        String link = frontendBaseUrl + "/reset-password?token=" + token;
        LOGGER.info("Reset Password Link: {}", link);

        try {
            mailService.send(bar.getEmail(), "Recuperar contraseña", "Enlace: " + link);
        } catch (Exception e) { /* ignorar */ }

        return Optional.of(token);
    }

    public boolean resetPasswordWithToken(String token, String newPassword) {
        Optional<PasswordResetToken> t = tokenRepository.findByToken(token);
        if (t.isEmpty()) return false;
        
        PasswordResetToken prt = t.get();
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(prt);
            return false;
        }

        Optional<Bar> barOpt = barRepository.findById(prt.getBarId());
        if (barOpt.isPresent()) {
            Bar bar = barOpt.get();
            bar.setPwd(passwordEncoder.encode(newPassword));
            barRepository.save(bar);
            tokenRepository.delete(prt);
            return true;
        }
        return false;
    }
}