package com.example.gramolaRodrigo.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.entities.EmailVerificationToken;
import com.example.gramolaRodrigo.entities.PasswordResetToken;
import com.example.gramolaRodrigo.repositories.BarRepository;
import com.example.gramolaRodrigo.repositories.EmailVerificationTokenRepository;
import com.example.gramolaRodrigo.repositories.PasswordResetTokenRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionRepository;

@Service
public class BarService {
    private final BarRepository barRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MailService mailService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public BarService(BarRepository br, PasswordEncoder pe, PasswordResetTokenRepository tr,
                      EmailVerificationTokenRepository etr, SubscriptionRepository sr, MailService ms) {
        this.barRepository = br;
        this.passwordEncoder = pe;
        this.tokenRepository = tr;
        this.emailTokenRepository = etr;
        this.subscriptionRepository = sr;
        this.mailService = ms;
    }

    @Transactional
    @SuppressWarnings("null")
    public Bar registerBar(String name, String email, String pwd, String clientId, String clientSecret) {
        barRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.isVerified() || !hasActiveSubscription(existing.getId())) {
                String id = existing.getId();
                emailTokenRepository.findAll().stream().filter(t -> id.equals(t.getBarId())).forEach(emailTokenRepository::delete);
                tokenRepository.findAll().stream().filter(t -> id.equals(t.getBarId())).forEach(tokenRepository::delete);
                subscriptionRepository.findAll().stream().filter(s -> s.getBar() != null && id.equals(s.getBar().getId())).forEach(subscriptionRepository::delete);
                barRepository.delete(existing);
            } else {
                throw new IllegalStateException("El email ya está registrado");
            }
        });

        Bar bar = new Bar();
        bar.setId(UUID.randomUUID().toString());
        bar.setName(name);
        bar.setEmail(email);
        bar.setPwd(passwordEncoder.encode(pwd));
        if (clientId != null) bar.setClientId(clientId);
        if (clientSecret != null) bar.setClientSecret(clientSecret);

        Bar saved = barRepository.save(bar);
        String token = createEmailVerificationForBar(saved);
        mailService.send(saved.getEmail(), "Confirma tu cuenta", frontendBaseUrl + "/confirm-email?token=" + token);
        return saved;
    }

    @SuppressWarnings("null")
    public boolean hasActiveSubscription(String barId) {
        Instant now = Instant.now();
        return subscriptionRepository.findAll().stream()
            .anyMatch(s -> s.getBar() != null && barId.equals(s.getBar().getId()) 
                && "ACTIVE".equalsIgnoreCase(s.getStatus()) 
                && (s.getEndAt() == null || s.getEndAt().isAfter(now)));
    }

    @Transactional
    @SuppressWarnings("null")
    public String createEmailVerificationForBar(Bar bar) {
        String token = UUID.randomUUID().toString();
        EmailVerificationToken evt = new EmailVerificationToken();
        evt.setToken(token);
        evt.setBarId(bar.getId());
        evt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        emailTokenRepository.save(evt);
        return token;
    }

    @Transactional
    @SuppressWarnings("null")
    public Optional<Bar> confirmEmailAndGetBar(String token) {
        return emailTokenRepository.findByToken(token).flatMap(evt -> {
            if (evt.getExpiresAt().isBefore(Instant.now())) return Optional.empty();
            return barRepository.findById(evt.getBarId()).map(bar -> {
                bar.setVerified(true);
                bar.setVerifiedAt(Instant.now());
                barRepository.save(bar);
                return bar;
            });
        });
    }

    public Optional<Bar> loginBar(String email, String pwd) {
        return barRepository.findByEmail(email)
            .filter(bar -> passwordEncoder.matches(pwd, bar.getPwd()) && bar.isVerified());
    }

    @Transactional
    @SuppressWarnings("null")
    public Optional<String> createPasswordResetTokenForEmail(String email) {
        return barRepository.findByEmail(email).map(bar -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(token);
            prt.setBarId(bar.getId());
            prt.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            tokenRepository.save(prt);
            mailService.send(bar.getEmail(), "Recuperar contraseña", frontendBaseUrl + "/reset-password?token=" + token);
            return token;
        });
    }

    @Transactional
    @SuppressWarnings("null")
    public boolean resetPasswordWithToken(String token, String newPassword) {
        return tokenRepository.findByToken(token).flatMap(prt -> {
            if (prt.getExpiresAt().isBefore(Instant.now())) return Optional.empty();
            return barRepository.findById(prt.getBarId()).map(bar -> {
                bar.setPwd(passwordEncoder.encode(newPassword));
                barRepository.save(bar);
                tokenRepository.delete(prt);
                return true;
            });
        }).orElse(false);
    }
    
    public void resendEmailVerification(String email) {
        barRepository.findByEmail(email).ifPresent(bar -> {
            if (!bar.isVerified()) {
                String token = createEmailVerificationForBar(bar);
                mailService.send(bar.getEmail(), "Confirma tu email", frontendBaseUrl + "/confirm-email?token=" + token);
            }
        });
    }

    @SuppressWarnings("null")
    public Map<String, Object> getEmailVerificationStatus(String token) {
        Map<String, Object> res = new HashMap<>();
        emailTokenRepository.findByToken(token).ifPresent(evt -> {
            res.put("status", "found");
            res.put("expired", evt.getExpiresAt().isBefore(Instant.now()));
        });
        return res;
    }
}