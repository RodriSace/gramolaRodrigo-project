package com.example.gramolaRodrigo.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/songs")
public class SongController {

    private final String DEEZER_API_URL = "https://api.deezer.com/search?q=";

    @GetMapping("/search")
    public ResponseEntity<String> searchSongs(@RequestParam("q") String query) {
        // Validate query
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\": \"Query parameter is required\"}");
        }
        if (query.trim().length() < 3) {
            return ResponseEntity.badRequest().body("{\"error\": \"Query must be at least 3 characters long\"}");
        }

        RestTemplate restTemplate = new RestTemplate();
        String cleanedQuery = query.trim().split(":")[0].replaceAll("[^a-zA-Z0-9\\s]", "");
        String deezerUrl = DEEZER_API_URL + cleanedQuery;

        try {
            // Hacemos la llamada a la API de Deezer y devolvemos la respuesta directamente
            String response = restTemplate.getForObject(deezerUrl, String.class);
            return ResponseEntity.ok(response);
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Failed to fetch songs from Deezer API\"}");
        }
    }
}
