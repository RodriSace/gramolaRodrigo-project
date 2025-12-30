package com.example.gramolaRodrigo.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "email_verification_tokens")
@Data
public class EmailVerificationToken {

    @Id
    private String token;

    @Column(nullable = false)
    private String barId;

    @Column(nullable = false)
    private Instant expiresAt;

    // Para idempotencia: marcamos si ya se usó en vez de borrar la fila
    private boolean used = false;
    private Instant usedAt;
}
