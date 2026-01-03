package com.example.gramolaRodrigo.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "playback_state")
@Data
public class PlaybackState {

    @Id
    @Column(length = 64)
    private String id; // singleton id e.g. 'global'

    private boolean paused;

    private String currentSongId;

    private double currentTimeSeconds;

    private Instant updatedAt;
}
