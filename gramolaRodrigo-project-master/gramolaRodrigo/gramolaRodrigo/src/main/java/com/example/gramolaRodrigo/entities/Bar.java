package com.example.gramolaRodrigo.entities;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="bars", indexes = {
    @Index(columnList = "email", unique = true, name = "unique_email")
})
@Data
@NoArgsConstructor
public class Bar {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String pwd;

    // Campos de Spotify para actuar en nombre del bar (permitimos null para compatibilidad)
    @Column(name = "spotify_client_id")
    private String clientId;

    @JsonIgnore
    @Column(name = "spotify_client_secret")
    private String clientSecret;
    
    @Column(nullable = false)
    private boolean verified = false;

    private Instant verifiedAt;
}