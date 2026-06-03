package com.gramola.backend.controllers;

import com.gramola.backend.models.SpotiToken;
import com.gramola.backend.services.SpotiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spoti")
@CrossOrigin(origins = "http://127.0.0.1:4200") // Permite peticiones desde el frontend en Angular
public class SpotiController {

    @Autowired
    private SpotiService service; // Servicio de integración con las APIs de Spotify

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 4 (Controller Canje)
    // El backend canjea el código por tokens definitivos de Spotify y los guarda en MySQL
    // Elemento: SpotiController.getAuthorizationToken() con @GetMapping("/getAuthorizationToken")
    // ========================================================
    /**
     * Intercambia el código temporal por tokens definitivos de Spotify.
     */
    @GetMapping("/getAuthorizationToken")
    public SpotiToken getAuthorizationToken(@RequestParam String code, @RequestParam String clientId, @RequestParam String email) {
        return service.getAuthorizationToken(code, clientId, email); // Intercambio seguro OAuth2
    }

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — REFRESO DE TOKENS (Controller)
    // Recibe el email y llama al servicio para refrescar el access token usando el refresh_token
    // Elemento: SpotiController.refreshToken() con @GetMapping("/refreshToken")
    // ========================================================
    /**
     * Refresca el token de acceso de Spotify para un bar determinado.
     */
    @GetMapping("/refreshToken")
    public SpotiToken refreshToken(@RequestParam String email) {
        return service.refreshToken(email); // Solicita una nueva llave de audio temporal
    }
}
