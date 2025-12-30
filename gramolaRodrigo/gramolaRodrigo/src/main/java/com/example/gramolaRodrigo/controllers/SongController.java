package com.example.gramolaRodrigo.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/songs")
public class SongController {

    private final String DEEZER_API_URL = "https://api.deezer.com/search?q=";

    @GetMapping("/search")
    public String searchSongs(@RequestParam String query) {
        RestTemplate restTemplate = new RestTemplate();
        String deezerUrl = DEEZER_API_URL + query;

        // Hacemos la llamada a la API de Deezer y devolvemos la respuesta directamente
        return restTemplate.getForObject(deezerUrl, String.class);
    }
}