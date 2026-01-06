package com.example.gramolaRodrigo.controllers;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
// Mantenemos /api/deezer porque el frontend parece buscarlo ahí para los audios
@RequestMapping("/api/deezer") 
public class DeezerController {
    private static final Logger logger = LoggerFactory.getLogger(DeezerController.class);

    @GetMapping("/preview/{id}")
    public ResponseEntity<Void> streamPreview(@PathVariable String id) {
        try {
            // 1. Preguntar a Deezer dónde está el audio
            String apiUrl = "https://api.deezer.com/track/" + id;
            ObjectMapper mapper = new ObjectMapper();
            Map<?, ?> track = mapper.readValue(new URL(apiUrl), Map.class);
            
            if (track == null || !track.containsKey("preview")) {
                return ResponseEntity.notFound().build();
            }

            String previewUrl = track.get("preview").toString();
            
            // 2. REDIRECCIÓN (302): "Navegador, ve tú a por el audio a esta URL"
            // Esto evita que tu servidor Java se sature y explote (Error 500)
            logger.info("Redirigiendo audio ID {} a: {}", id, previewUrl);
            
            return ResponseEntity.status(HttpStatus.FOUND) // 302 Found
                    .location(URI.create(previewUrl))
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .build();

        } catch (Exception e) {
            logger.error("Error en DeezerController: {}", e.getMessage());
            // Si falla, devolvemos 404 en vez de 500 para no romper el cliente
            return ResponseEntity.notFound().build();
        }
    }
}