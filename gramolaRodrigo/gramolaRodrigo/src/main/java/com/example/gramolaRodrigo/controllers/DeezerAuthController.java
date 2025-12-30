package com.example.gramolaRodrigo.controllers;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.services.DeezerAuthService;

@RestController
public class DeezerAuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeezerAuthController.class);
    private final DeezerAuthService deezer;
    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    public DeezerAuthController(DeezerAuthService deezer) {
        this.deezer = deezer;
    }

    // Step 1: Redirect user to Deezer consent screen. Pass barId in state.
    @GetMapping("/deezer/login")
    public ResponseEntity<Void> login(@RequestParam("barId") String barId) {
        if (!deezer.isConfigured()) {
            // Avoid creating a broken Deezer URL when configuration is missing
            return ResponseEntity.status(400).build();
        }
        String url = deezer.buildAuthRedirectUrl(barId);
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    // Step 2: Deezer redirects back here with ?code=...&state=...
    // Note: The path is /auth to match the recommended redirect URL in Deezer settings
    @GetMapping(path = "/auth", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> callback(@RequestParam("code") String code,
                                           @RequestParam(value = "state", required = false) String state) {
        Optional<String> maybeToken = deezer.exchangeCodeForToken(code);
        if (maybeToken.isEmpty()) {
            return ResponseEntity.status(400).body("<html><body><h3>Deezer auth failed</h3><p>Could not get access token.</p></body></html>");
        }
        String token = maybeToken.get();
        if (state != null && !state.isBlank()) {
            deezer.saveTokenForBar(state, token);
            LOGGER.info("Saved Deezer access token for barId={}", state);
        }
        // Redirigir de nuevo a la app frontend con un indicador opcional
        String redirect = frontendBaseUrl;
        if (!redirect.endsWith("/")) redirect = redirect + "/";
        redirect = redirect + "?deezer=connected";
        return ResponseEntity.status(302).location(URI.create(redirect)).build();
    }

    // Optional: quick check to get current user info using the stored token
    @GetMapping("/deezer/me")
    public ResponseEntity<?> me(@RequestParam("barId") String barId) {
        // If Deezer OAuth isn't configured, tell client to skip OAuth silently
        if (!deezer.isConfigured()) {
            return ResponseEntity.noContent().build(); // 204 → frontend won't attempt to OAuth
        }
        Optional<String> token = deezer.getTokenForBar(barId);
        if (token.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "No token stored for this barId. Login first."));
        }
        return deezer.fetchMe(token.get())
                .<ResponseEntity<?>>map(body -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body))
                .orElseGet(() -> ResponseEntity.status(502).body(Map.of("error", "Failed to fetch Deezer user")));
    }
}
