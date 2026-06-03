package com.gramola.backend.models;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad JPA que representa a los usuarios (locales/bares) registrados en el sistema.
 */
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String barName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean confirmed = false;
    private String confirmationToken;
    private String resetPasswordToken;

    private boolean subscriptionActive = false;

    private String spotifyClientId;
    private String spotifyClientSecret;

    @Column(length = 2000)
    private String spotifyAccessToken;

    @Column(length = 2000)
    private String spotifyRefreshToken;
    
    @Column(nullable = false)
    private long songPriceCents = 100L; 

    private String currentPlaylistUri;
    private Integer lastPlaylistIndex = 0;
}
