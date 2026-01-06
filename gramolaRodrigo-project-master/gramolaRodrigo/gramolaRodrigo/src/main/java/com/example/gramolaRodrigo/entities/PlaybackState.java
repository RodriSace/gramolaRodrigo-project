package com.example.gramolaRodrigo.entities;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "playback_state")
public class PlaybackState {
    @Id
    private String id; // Always "global"

    private boolean paused;
    private String currentSongId;
    private double currentTimeSeconds;
    private Instant updatedAt;
    
    // Tracks the current position in the background playlist
    private int currentPlaylistIndex = 0;

    public PlaybackState() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public String getCurrentSongId() { return currentSongId; }
    public void setCurrentSongId(String currentSongId) { this.currentSongId = currentSongId; }
    public double getCurrentTimeSeconds() { return currentTimeSeconds; }
    public void setCurrentTimeSeconds(double currentTimeSeconds) { this.currentTimeSeconds = currentTimeSeconds; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getCurrentPlaylistIndex() { return currentPlaylistIndex; }
    public void setCurrentPlaylistIndex(int currentPlaylistIndex) { this.currentPlaylistIndex = currentPlaylistIndex; }
}