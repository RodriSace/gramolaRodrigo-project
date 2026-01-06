package com.example.gramolaRodrigo.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.entities.QueuedSong;
import com.example.gramolaRodrigo.services.QueueService;

@RestController
@RequestMapping("/queue")
public class QueueController {

    private static final Logger logger = LoggerFactory.getLogger(QueueController.class);
    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getQueue() {
        try {
            List<QueuedSong> songs = queueService.getQueue();
            List<Map<String, Object>> response = new ArrayList<>();

            if (songs != null) {
                for (QueuedSong s : songs) {
                    // Protección contra Nulos: Si un elemento de la lista es null, lo saltamos
                    if (s != null) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId());
                        map.put("songId", s.getSongId());
                        map.put("title", s.getTitle());
                        map.put("artist", s.getArtist());
                        map.put("albumCover", s.getAlbumCover());
                        map.put("previewUrl", s.getPreviewUrl());
                        map.put("duration", s.getDuration());
                        // Si 'position' es null o 0, ponemos 0
                        map.put("position", s.getPosition());
                        
                        response.add(map);
                    }
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("ERROR CRÍTICO EN GET /queue: ", e);
            // Devolver lista vacía en lugar de explotar con Error 500
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping("/add")
    public void addSong(@RequestBody QueuedSong song) {
        try {
            queueService.addSongToQueue(song.getSongId(), song.getTitle(), song.getArtist(), 
                                      song.getAlbumCover(), song.getPreviewUrl(), song.getDuration());
        } catch (Exception e) {
            logger.error("Error al añadir canción: ", e);
        }
    }

    @PostMapping("/next")
    public ResponseEntity<Map<String, Object>> playNext() {
        try {
            return queueService.playNextSong()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("songId", s.getSongId());
                    map.put("title", s.getTitle());
                    map.put("artist", s.getArtist());
                    map.put("albumCover", s.getAlbumCover());
                    map.put("previewUrl", s.getPreviewUrl());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            logger.error("Error en /next: ", e);
            return ResponseEntity.noContent().build();
        }
    }
}