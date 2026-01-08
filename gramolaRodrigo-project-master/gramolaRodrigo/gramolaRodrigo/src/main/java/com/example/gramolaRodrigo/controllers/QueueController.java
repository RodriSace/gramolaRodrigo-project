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
// SEGURIDAD: NO añadir @CrossOrigin aquí. Se gestiona en SecurityConfig para evitar errores de credenciales.
public class QueueController {

    private static final Logger logger = LoggerFactory.getLogger(QueueController.class);
    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * Obtiene la lista actual de la cola.
     * Mapea manualmente a Map para evitar errores de serialización JSON y punteros nulos.
     */
    @GetMapping
    @SuppressWarnings("null") // Elimina avisos de análisis de nulos del editor.
    public ResponseEntity<List<Map<String, Object>>> getQueue() {
        try {
            List<QueuedSong> songs = queueService.getQueue();
            List<Map<String, Object>> response = new ArrayList<>();

            if (songs != null) {
                for (QueuedSong s : songs) {
                    if (s != null) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", s.getId());
                        map.put("songId", s.getSongId());
                        map.put("title", s.getTitle());
                        map.put("artist", s.getArtist());
                        map.put("albumCover", s.getAlbumCover());
                        map.put("previewUrl", s.getPreviewUrl());
                        map.put("duration", s.getDuration());
                        map.put("position", s.getPosition());
                        
                        response.add(map);
                    }
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("ERROR CRÍTICO EN GET /queue: ", e);
            // Fallback seguro: devolver lista vacía en lugar de error 500 para no romper el frontend
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * Añade una canción a la cola de reproducción.
     */
    @PostMapping("/add")
    @SuppressWarnings("null")
    public ResponseEntity<Void> addSong(@RequestBody QueuedSong song) {
        try {
            if (song == null || song.getSongId() == null) {
                return ResponseEntity.badRequest().build();
            }

            queueService.addSongToQueue(
                song.getSongId(), 
                song.getTitle(), 
                song.getArtist(), 
                song.getAlbumCover(), 
                song.getPreviewUrl(), 
                song.getDuration()
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error al añadir canción a la cola: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Avanza a la siguiente canción de la cola.
     */
    @PostMapping("/next")
    @SuppressWarnings("null")
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
                    map.put("duration", s.getDuration());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            logger.error("Error en /next: ", e);
            return ResponseEntity.noContent().build();
        }
    }
}