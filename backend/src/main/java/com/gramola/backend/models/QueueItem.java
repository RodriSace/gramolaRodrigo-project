package com.gramola.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad JPA vinculada a la tabla 'playback_queue' que representa una canción en la cola de reproducción activa de un bar.
 */
@Entity
@Table(name = "playback_queue")
@Data
public class QueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bar_id")
    private User bar;

    private String spotifyTrackId;
    private String title;
    private String artist;
    private String previewUrl;
    private String albumArtUrl;
    private long durationMs;

    private int position; 
    private boolean isPaid; 
    private LocalDateTime addedAt;
}

