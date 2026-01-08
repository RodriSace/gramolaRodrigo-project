package com.example.gramolaRodrigo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "queued_song")
@Data
@NoArgsConstructor
public class QueuedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String songId; // El ID original que viene de la API de Deezer
    private String title;
    private String artist;

    @Column(length = 1024)
    private String albumCover;

    private boolean hasPlayed = false; // Indica si la canción ya ha sido reproducida

    @Column(columnDefinition = "TEXT") // CAMBIO CRÍTICO: Evita el truncado de URLs largas de Deezer
    private String previewUrl;

    private int duration; // Duración en segundos (generalmente 30s para previews)
    
    private int position; // Utilizado para ordenar la cola cronológicamente
}