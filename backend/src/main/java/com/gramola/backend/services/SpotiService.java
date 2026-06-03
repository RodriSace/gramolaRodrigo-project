package com.gramola.backend.services;

import com.gramola.backend.models.SpotiToken;
import com.gramola.backend.models.User;
import com.gramola.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class SpotiService {

    @Autowired
    private UserRepository userRepository; // Acceso a base de datos de usuarios

    private final String tokenUrl = "https://accounts.spotify.com"; // Endpoint de tokens de Spotify

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 4 (Service Canje)
    // Lee el clientSecret de MySQL, prepara la petición HTTP POST asíncrona a accounts.spotify.com
    // y persiste los tokens de acceso y refresco devueltos en MySQL
    // ========================================================
    /**
     * Intercambia el código de autorización temporal por los tokens de acceso y refresco de Spotify.
     */
    public SpotiToken getAuthorizationToken(String code, String clientId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String clientSecret = user.getSpotifyClientSecret();

        // Petición encoded en formato formulario URL
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", "http://127.0.0.1:4200/callback");

        // Cabecera de autenticación básica (Basic Auth Base64)
        String header = this.basicAuth(clientId, clientSecret);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, header);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        
        ResponseEntity<SpotiToken> response = restTemplate.postForEntity(tokenUrl + "/api/token", request, SpotiToken.class);

        SpotiToken token = response.getBody();
        if (token != null) {
            user.setSpotifyAccessToken(token.getAccessToken());
            user.setSpotifyRefreshToken(token.getRefreshToken());
            userRepository.save(user);
        }
        return token;
    }

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — REFRESO DE TOKENS (Service)
    // Utiliza el refresh token de MySQL para realizar el intercambio seguro de tokens con Spotify
    // ========================================================
    /**
     * Utiliza el refresh token guardado para solicitar a Spotify un nuevo access token.
     */
    public SpotiToken refreshToken(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String clientId = user.getSpotifyClientId();
        String clientSecret = user.getSpotifyClientSecret();
        String refreshToken = user.getSpotifyRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new RuntimeException("No se encontró ningún Refresh Token de Spotify para este local");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        String header = this.basicAuth(clientId, clientSecret);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, header);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        
        ResponseEntity<SpotiToken> response = restTemplate.postForEntity(tokenUrl + "/api/token", request, SpotiToken.class);

        SpotiToken token = response.getBody();
        if (token != null) {
            user.setSpotifyAccessToken(token.getAccessToken());
            if (token.getRefreshToken() != null && !token.getRefreshToken().isEmpty()) {
                user.setSpotifyRefreshToken(token.getRefreshToken());
            }
            userRepository.save(user);
        }
        return token;
    }

    private String basicAuth(String clientId, String clientSecret) {
        String pair = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
    }

    // ========================================================
    // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 1 (Spotify API)
    // Realiza el GET Bearer HTTP a api.spotify.com/v1/search para buscar canciones
    // ========================================================
    /**
     * Realiza una búsqueda de canciones en la API de Spotify.
     */
    public Object searchTracks(String query, String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://api.spotify.com/v1/search?q=" + encodedQuery + "&type=track&limit=10";
            return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class).getBody();
        } catch (Exception e) {
            System.err.println("Error buscando canciones en la API de Spotify: " + e.getMessage());
            return Map.of("error", "Error de búsqueda en Spotify");
        }
    }
}

