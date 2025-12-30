package com.example.gramolaRodrigo.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping; // <-- AÑADIR
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.entities.QueuedSong;
import com.example.gramolaRodrigo.services.QueueService;

@RestController
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public ResponseEntity<List<QueuedSong>> getQueue() {
        List<QueuedSong> queue = queueService.getQueue();
        return ResponseEntity.ok(queue);
    }

    // 👇 ENDPOINT NUEVO 👇
    @PostMapping("/next")
    public ResponseEntity<?> playNextSong() {
        return ResponseEntity.ok(queueService.playNextSong());
    }

    // Estado global de pause/play
    @GetMapping("/playback-state")
    public ResponseEntity<?> getPlaybackState() {
        return ResponseEntity.ok(java.util.Map.of("paused", queueService.isGloballyPaused()));
    }

    @PostMapping("/toggle-playback")
    public ResponseEntity<?> togglePlayback() {
        queueService.toggleGlobalPause();
        return ResponseEntity.ok(java.util.Map.of("paused", queueService.isGloballyPaused()));
    }

    // Guardar posición de reproducción (se llama desde el cliente onbeforeunload)
    @PostMapping("/save-position")
    public ResponseEntity<?> savePlaybackPosition(@RequestBody Map<String, Object> payload) {
        try {
            String songId = (String) payload.getOrDefault("songId", null);
            double currentTime = 0.0;
            Object t = payload.get("currentTime");
            if (t instanceof Number) {
                currentTime = ((Number) t).doubleValue();
            } else if (t instanceof String) {
                currentTime = Double.parseDouble((String) t);
            }
            boolean paused = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("paused", "false")));
            queueService.savePlaybackPosition(songId, currentTime, paused);
            return ResponseEntity.ok(Map.of("saved", true));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/load-position")
    public ResponseEntity<?> loadPlaybackPosition() {
        return ResponseEntity.ok(queueService.loadPlaybackState().map(s -> java.util.Map.of(
                "songId", s.getCurrentSongId(),
                "currentTime", s.getCurrentTimeSeconds(),
                "paused", s.isPaused()
        )).orElse(java.util.Map.of()));
    }
}