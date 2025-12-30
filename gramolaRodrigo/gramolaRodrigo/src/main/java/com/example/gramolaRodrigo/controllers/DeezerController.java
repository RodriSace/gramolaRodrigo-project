package com.example.gramolaRodrigo.controllers;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/deezer")
public class DeezerController {
    private static final Logger logger = LoggerFactory.getLogger(DeezerController.class);

    /**
     * Proxy endpoint that returns the 30s preview audio bytes for a Deezer track.
     * Example: GET /api/deezer/preview/3135556
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<StreamingResponseBody> streamPreview(@PathVariable String id) {
        try {
            // 1) Fetch track metadata from Deezer to obtain the preview URL
            String trackUrl = "https://api.deezer.com/track/" + id;
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> track = mapper.readValue(new URL(trackUrl), Map.class);
            if (track == null || !track.containsKey("preview")) {
                return ResponseEntity.notFound().build();
            }

            Object previewObj = track.get("preview");
            if (previewObj == null) {
                return ResponseEntity.notFound().build();
            }

            String previewUrl = previewObj.toString();
            logger.info("Proxying Deezer preview for id={} -> {}", id, previewUrl);

            // 2) Open connection and stream bytes through to the client
            URL url = new URL(previewUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setRequestProperty("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.8");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int code = conn.getResponseCode();
            if (code >= 400) {
                logger.error("Deezer preview URL returned error code {} for id={}", code, id);
                return ResponseEntity.status(502).build();
            }

            String contentTypeStr = conn.getContentType();
            MediaType contentType = (contentTypeStr != null) ? MediaType.parseMediaType(contentTypeStr)
                    : MediaType.valueOf("audio/mpeg");

            InputStream is = conn.getInputStream();

            StreamingResponseBody body = outputStream -> {
                try (InputStream in = is) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                } finally {
                    try {
                        conn.disconnect();
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);

            return ResponseEntity.ok().headers(headers).body(body);
        } catch (Exception e) {
            logger.error("Unexpected error in DeezerController streaming: {}", e.getMessage(), e);
            return ResponseEntity.status(502).build();
        }
    }

    // Diagnostic endpoint: returns the preview URL and HTTP status for the preview resource
    @GetMapping("/check/{id}")
    public ResponseEntity<Map<String, Object>> checkPreview(@PathVariable String id) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String trackUrl = "https://api.deezer.com/track/" + id;
            Map<?, ?> track = mapper.readValue(new URL(trackUrl), Map.class);
            if (track == null || !track.containsKey("preview")) {
                return ResponseEntity.ok(Map.of("found", false, "message", "no preview in metadata"));
            }
            String previewUrl = String.valueOf(track.get("preview"));
            HttpURLConnection conn = (HttpURLConnection) new URL(previewUrl).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int code = conn.getResponseCode();
            String ctype = conn.getContentType();
            conn.disconnect();
            return ResponseEntity.ok(Map.of("found", true, "previewUrl", previewUrl, "status", code, "contentType", ctype));
        } catch (Exception e) {
            logger.warn("Error checking preview for id={}: {}", id, e.getMessage());
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }
}
