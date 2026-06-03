package com.gramola.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad JPA vinculada a la tabla 'bar_songs' para registrar el historial de reproducción de cada bar.
 */
@Entity
@Table(name = "bar_songs")
@Data
public class BarSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bar_id")
    private User bar;

    private String spotifyTrackId;
    private String title;
    private String artist;
    private LocalDateTime playedAt;
}

