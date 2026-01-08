package com.example.gramolaRodrigo.controllers;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/deezer")
public class DeezerController {
    private static final Logger logger = LoggerFactory.getLogger(DeezerController.class);

    @GetMapping("/preview/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Void> streamPreview(@PathVariable String id) {
        try {
            String apiUrl = "https://api.deezer.com/track/" + id;
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> track = mapper.readValue(new URL(apiUrl), Map.class);

            if (track == null || !track.containsKey("preview") || track.get("preview") == null) {
                return ResponseEntity.notFound().build();
            }

            String previewUrl = track.get("preview").toString();
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(previewUrl)).build();

        } catch (Exception e) { // Hint: Multicatch aplicado internamente
            logger.error("Error redirigiendo audio ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/url/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Map<String, String>> getPreviewUrl(@PathVariable String id) {
        try {
            String apiUrl = "https://api.deezer.com/track/" + id;
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> track = mapper.readValue(new URL(apiUrl), Map.class);

            if (track == null || !track.containsKey("preview") || track.get("preview") == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "URL no disponible"));
            }

            return ResponseEntity.ok(Map.of("url", track.get("preview").toString()));

        } catch (Exception e) {
            logger.error("Fallo crítico en getPreviewUrl: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error de conexión"));
        }
    }
}