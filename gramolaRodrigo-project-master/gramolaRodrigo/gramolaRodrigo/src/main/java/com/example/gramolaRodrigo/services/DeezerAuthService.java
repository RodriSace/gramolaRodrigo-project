package com.example.gramolaRodrigo.services;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DeezerAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeezerAuthService.class);

    @Value("${deezer.app-id:}")
    private String appId;

    @Value("${deezer.secret:}")
    private String secret;

    @Value("${deezer.redirect-url:http://127.0.0.1:8080/auth}")
    private String redirectUrl;

    // Simple in-memory token store for demo/exam purposes (keyed by barId)
    private final Map<String, String> accessTokens = new ConcurrentHashMap<>();

    public String buildAuthRedirectUrl(String state) {
        // If not configured, return empty so caller can handle gracefully
        if (!isConfigured()) {
            LOGGER.warn("Deezer OAuth not configured: appId/secret/redirectUrl missing");
            return "";
        }
        String perms = "basic_access,email"; // minimal perms; add more if needed
        String base = "https://connect.deezer.com/oauth/auth.php";
        String url = base + "?app_id=" + enc(appId)
                + "&redirect_uri=" + enc(redirectUrl)
                + "&perms=" + enc(perms)
                + "&response_type=code"
                + (state != null ? ("&state=" + enc(state)) : "");
        LOGGER.info("Deezer auth URL generated: {}", url);
        return url;
    }

    public Optional<String> exchangeCodeForToken(String code) {
        try {
            String url = "https://connect.deezer.com/oauth/access_token.php?app_id=" + enc(appId)
                    + "&secret=" + enc(secret)
                    + "&code=" + enc(code)
                    + "&output=json";
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> resp = rt.getForEntity(URI.create(url), String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                LOGGER.warn("Token exchange failed: status={}, body={}", resp.getStatusCode(), resp.getBody());
                return Optional.empty();
            }
            // Very small parse: look for access_token":"..."
            String body = resp.getBody();
            String token = extractField(body, "access_token");
            if (token == null || token.isBlank()) {
                LOGGER.warn("No access_token in Deezer response: {}", body);
                return Optional.empty();
            }
            return Optional.of(token);
        } catch (Exception e) {
            LOGGER.error("Error exchanging Deezer code for token", e);
            return Optional.empty();
        }
    }

    public void saveTokenForBar(String barId, String token) {
        if (barId != null && token != null) {
            accessTokens.put(barId, token);
        }
    }

    public Optional<String> getTokenForBar(String barId) {
        return Optional.ofNullable(accessTokens.get(barId));
    }

    public Optional<String> fetchMe(String accessToken) {
        try {
            String url = "https://api.deezer.com/user/me?access_token=" + enc(accessToken);
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> resp = rt.getForEntity(URI.create(url), String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) return Optional.empty();
            return Optional.ofNullable(resp.getBody());
        } catch (Exception e) {
            LOGGER.error("Error fetching Deezer user/me", e);
            return Optional.empty();
        }
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private static String extractField(String json, String field) {
        // naive field extraction to avoid adding JSON libs here; acceptable for controlled responses
        String q = "\"" + field + "\":";
        int i = json.indexOf(q);
        if (i < 0) return null;
        int start = json.indexOf('"', i + q.length());
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end);
    }

    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && secret != null && !secret.isBlank()
                && redirectUrl != null && !redirectUrl.isBlank();
    }
}
