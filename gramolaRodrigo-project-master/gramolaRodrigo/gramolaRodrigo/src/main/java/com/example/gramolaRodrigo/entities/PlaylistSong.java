package com.example.gramolaRodrigo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "playlist_songs")
public class PlaylistSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String songId;
    private String title;
    private String artist;

    @Column(length = 1024) // Soporta URLs de carátulas largas o de alta resolución
    private String albumCover;

    @Column(columnDefinition = "TEXT") // CAMBIO CRÍTICO: Evita errores de truncamiento con URLs de Deezer
    private String previewUrl;

    private int duration;
    
    /**
     * Índice para mantener el orden de reproducción de la lista de fondo
     * (background playlist) gestionada por el QueueService.
     */
    private int playlistIndex;

    public PlaylistSong() {}

    // --- GETTERS Y SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbumCover() { return albumCover; }
    public void setAlbumCover(String albumCover) { this.albumCover = albumCover; }

    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getPlaylistIndex() { return playlistIndex; }
    public void setPlaylistIndex(int playlistIndex) { this.playlistIndex = playlistIndex; }
}