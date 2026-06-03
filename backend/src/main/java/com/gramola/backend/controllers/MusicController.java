package com.gramola.backend.controllers;

import com.gramola.backend.models.QueueItem;
import com.gramola.backend.models.User;
import com.gramola.backend.repositories.UserRepository;
import com.gramola.backend.services.QueueService;
import com.gramola.backend.services.SpotiService;
import com.gramola.backend.services.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music")
@CrossOrigin(origins = "http://127.0.0.1:4200") 
public class MusicController {

    @Autowired
    private SpotiService spotiService; // Servicio para llamadas a la API de Spotify

    @Autowired
    private QueueService queueService; // Servicio para el manejo de la cola de canciones

    @Autowired
    private UserRepository userRepository; // Acceso a la base de datos de usuarios

    @Autowired
    private StripeService stripeService; // Servicio para procesar pagos con Stripe

    // ========================================================
    // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 3 (Controller Cola)
    // Devuelve la lista ordenada (cola) de canciones asociadas a un local
    // Elemento: `queueService.getQueue(barId)` en Angular hace GET a `/api/music/queue` -> llama a getQueue()
    // ========================================================
    /**
     * Obtiene la cola de reproducción del bar.
     */
    @GetMapping("/queue")
    public List<QueueItem> getQueue(@RequestParam Long barId) {
        User bar = userRepository.findById(barId).orElseThrow();
        return queueService.getQueueForBar(bar); // Devuelve la lista ordenada de canciones
    }

    // ========================================================
    // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 3 (Controller Volcado)
    // Vuelca todas las canciones de una playlist en MySQL de forma transaccional no pagada
    // Elemento: `selectPlaylist()` en Angular hace POST a `/api/music/queue/load-playlist` -> llama a loadPlaylist()
    // ========================================================
    /**
     * Carga una playlist de fondo en la cola del bar.
     */
    @PostMapping("/queue/load-playlist")
    public ResponseEntity<?> loadPlaylist(@RequestBody Map<String, Object> payload) {
        Long barId = Long.valueOf(payload.get("barId").toString());
        User bar = userRepository.findById(barId).orElseThrow();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tracks = (List<Map<String, Object>>) payload.get("tracks");
        
        System.out.println("[CONTROLLER] /queue/load-playlist - barId=" + barId + ", tracks recibidos=" + (tracks != null ? tracks.size() : "NULL"));
        if (tracks != null && !tracks.isEmpty()) {
            System.out.println("[CONTROLLER] Primera canción: " + tracks.get(0).get("title"));
        }
        
        queueService.loadPlaylistIntoQueue(bar, tracks); 
        return ResponseEntity.ok(Map.of("message", "Playlist cargada en la cola", "total", tracks.size()));
    }

    // ========================================================
    // 🔄 FLUJO 6: CUANDO UNA CANCIÓN TERMINA — PASO 3 (Controller)
    // Recibe el aviso de fin de canción de Angular, borra pos 0 de MySQL y desplaza el resto
    // Elemento: `finishCurrentSong()` en Angular hace POST a `/api/music/queue/finish` -> llama a finishSong()
    // ========================================================
    /**
     * Notifica la finalización de la canción en reproducción y avanza la cola.
     */
    @PostMapping("/queue/finish")
    public ResponseEntity<?> finishSong(@RequestBody Map<String, Long> payload) {
        Long barId = payload.get("barId");
        User bar = userRepository.findById(barId).orElseThrow();
        queueService.removeFirstAndAdvance(bar); 
        return ResponseEntity.ok(Map.of("message", "Canción terminada"));
    }

    // ========================================================
    // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 1 (Controller)
    // Busca canciones en la API de Spotify e inyecta la búsqueda asíncrona
    // Elemento: `executeSearch(query)` en Angular hace GET a `/api/music/search` -> llama a search()
    // ========================================================
    /**
     * Busca canciones en la API de Spotify.
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query, @RequestParam String token) {
        return ResponseEntity.ok(spotiService.searchTracks(query, token)); // Llama al buscador oficial de Spotify
    }

    // ========================================================
    // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 2 (Controller TPV)
    // Pide al SDK de Stripe crear la pasarela de pago único para la canción y devuelve la URL a Angular
    // Elemento: `paySong()` en Angular hace POST a `/api/music/pay` -> llama a payForSong()
    // ========================================================
    /**
     * Inicia el proceso de pago para añadir una canción a la cola.
     */
    @PostMapping("/pay")
    public ResponseEntity<?> payForSong(@RequestBody Map<String, Object> payload) throws Exception {
        Long barId = Long.valueOf(payload.get("barId").toString());
        User bar = userRepository.findById(barId).orElseThrow();
        
        String trackTitle = (String) payload.get("title");
        String trackId = (String) payload.get("trackId");
        String artist = (String) payload.get("artist");
        String previewUrl = (String) payload.get("previewUrl");
        String albumArtUrl = (String) payload.get("albumArtUrl");
        
        long durationMs = 180000L;
        Object durationObj = payload.get("durationMs");
        if (durationObj != null) {
            try {
                durationMs = Long.valueOf(durationObj.toString());
            } catch (Exception e) {
                // Silencioso
            }
        }

        String url = stripeService.createSongPaymentSession(
            trackTitle, barId, trackId, artist, previewUrl, albumArtUrl, durationMs, bar.getSongPriceCents()
        );
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ========================================================
    // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 3 (Controller Guardar)
    // Guarda una canción en la cola de MySQL. Si isPaid=true se cuela con prioridad VIP
    // Elemento: `musicService.addToQueue(payload)` en Angular hace POST a `/api/music/queue/add` -> llama a addToQueue()
    // ========================================================
    /**
     * Añade una canción a la cola de reproducción.
     */
    @PostMapping("/queue/add")
    public ResponseEntity<?> addToQueue(@RequestBody Map<String, Object> payload) {
        Long barId = Long.valueOf(payload.get("barId").toString());
        User bar = userRepository.findById(barId).orElseThrow();
        
        QueueItem item = new QueueItem();
        item.setTitle((String) payload.get("title"));
        item.setArtist((String) payload.get("artist"));
        item.setSpotifyTrackId((String) payload.get("trackId"));
        item.setPreviewUrl((String) payload.get("previewUrl"));
        item.setAlbumArtUrl((String) payload.get("albumArtUrl"));
        
        Object durationObj = payload.get("durationMs");
        long duration = 180000L; 
        if (durationObj != null) {
            try {
                duration = Long.valueOf(durationObj.toString());
            } catch (Exception e) {
                // Silencioso
            }
        }
        item.setDurationMs(duration);
        
        boolean isPaid = (boolean) payload.get("isPaid");
        
        queueService.addSongToQueue(bar, item, isPaid);
        return ResponseEntity.ok(Map.of("message", "Añadida a la cola"));
    }
}
