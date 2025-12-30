package com.example.gramolaRodrigo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class QueuedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String songId; // El ID que nos viene de la API de Deezer
    private String title;
    private String artist;
    @Column(length = 1024)
    private String albumCover;
    private boolean hasPlayed = false; // Para marcar si la canción ya ha sonado
    @Column(length = 2048)
    private String previewUrl; // URL al preview (30s) de Deezer
    private int duration; // duración en segundos (según Deezer)
}