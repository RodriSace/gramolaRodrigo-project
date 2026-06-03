package com.gramola.backend.repositories;

import com.gramola.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio de persistencia para la entidad User.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByConfirmationToken(String token);
    
    Optional<User> findByResetPasswordToken(String token);
    
    Optional<User> findBySpotifyClientId(String spotifyClientId);
}

