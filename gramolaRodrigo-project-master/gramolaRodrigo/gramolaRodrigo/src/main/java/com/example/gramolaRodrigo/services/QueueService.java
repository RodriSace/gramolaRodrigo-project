package com.example.gramolaRodrigo.services;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.entities.PlaybackState;
import com.example.gramolaRodrigo.entities.PlaylistSong;
import com.example.gramolaRodrigo.entities.QueuedSong;
import com.example.gramolaRodrigo.entities.Subscription;
import com.example.gramolaRodrigo.repositories.BarRepository;
import com.example.gramolaRodrigo.repositories.PlaybackStateRepository;
import com.example.gramolaRodrigo.repositories.PlaylistSongRepository;
import com.example.gramolaRodrigo.repositories.QueuedSongRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class QueueService {
    private final QueuedSongRepository queuedSongRepository;
    private final PlaybackStateRepository playbackStateRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final BarRepository barRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueService.class);

    public QueueService(QueuedSongRepository qs, PlaybackStateRepository ps, PlaylistSongRepository pls, 
                        BarRepository br, SubscriptionRepository sr, PasswordEncoder pe) {
        this.queuedSongRepository = qs;
        this.playbackStateRepository = ps;
        this.playlistSongRepository = pls;
        this.barRepository = br;
        this.subscriptionRepository = sr;
        this.passwordEncoder = pe;
    }

    @PostConstruct
    @Transactional
    public void init() {
        try {
            // 1. Limpieza para evitar residuos de sesiones anteriores
            subscriptionRepository.deleteAll();
            barRepository.deleteAll();
            playlistSongRepository.deleteAll();
            queuedSongRepository.deleteAll();
            playbackStateRepository.deleteAll();

            // 2. Configuración del Bar Maestro
            Bar bar = new Bar();
            bar.setId(UUID.randomUUID().toString());
            bar.setEmail("verse@ejemplo.com");
            bar.setName("Bar Verse");
            bar.setPwd(passwordEncoder.encode("1234"));
            bar.setVerified(true);
            bar.setVerifiedAt(Instant.now());
            barRepository.save(bar);

            Subscription sub = new Subscription();
            sub.setId(UUID.randomUUID().toString());
            sub.setBar(bar);
            sub.setStatus("ACTIVE");
            sub.setPlanId("ANNUAL");
            sub.setStartAt(Instant.now());
            sub.setEndAt(Instant.now().plus(Duration.ofDays(365)));
            subscriptionRepository.save(sub);

            // 3. Carga de la lista de reproducción
            initializeRealDeezerPlaylist();

            // 4. Estado inicial: Apuntamos al índice 0
            PlaybackState initialState = new PlaybackState();
            initialState.setId("global");
            initialState.setCurrentPlaylistIndex(0);
            // Buscamos el ID de la primera canción para sincronizar el estado inicial
            playlistSongRepository.findByPlaylistIndex(0).ifPresent(s -> initialState.setCurrentSongId(s.getSongId()));
            playbackStateRepository.save(initialState);

            LOGGER.info(">>> Backend Listo: Sistema de cola inicializado correctamente.");
        } catch (Exception e) {
            LOGGER.error("Fallo crítico en init: {}", e.getMessage());
        }
    }

    private void initializeRealDeezerPlaylist() {
        List<String[]> trackData = List.of(
            new String[]{"124895572", "Shape of You", "Ed Sheeran"},
            new String[]{"1109731", "Levitating", "Dua Lipa"},
            new String[]{"3135556", "Someone Like You", "Adele"},
            new String[]{"1109730", "Good 4 U", "Olivia Rodrigo"},
            new String[]{"3135557", "Rolling in the Deep", "Adele"},
            new String[]{"1109729", "Watermelon Sugar", "Harry Styles"},
            new String[]{"3135558", "Don't Start Now", "Dua Lipa"},
            new String[]{"1109728", "As It Was", "Harry Styles"},
            new String[]{"3135559", "Heat Waves", "Glass Animals"},
            new String[]{"1109727", "Stay", "The Kid Laroi"},
            new String[]{"1109732", "Peaches", "Justin Bieber"},
            new String[]{"3135560", "Bad Habit", "Steve Lacy"},
            new String[]{"1109733", "Permission to Dance", "BTS"},
            new String[]{"3135561", "Industry Baby", "Lil Nas X"}
        );

        int index = 0;
        for (String[] data : trackData) {
            saveWithFallback(data[0], data[1], data[2], index++);
        }
    }

    private void saveWithFallback(String id, String title, String artist, int idx) {
        PlaylistSong s = new PlaylistSong();
        s.setSongId(id); s.setPlaylistIndex(idx); s.setDuration(30);
        s.setTitle(title); s.setArtist(artist);
        
        try {
            URL url = new URL("https://api.deezer.com/track/" + id);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(2000);
            if (con.getResponseCode() == 200) {
                Map<?, ?> map = new ObjectMapper().readValue(con.getInputStream(), Map.class);
                s.setPreviewUrl((String) map.get("preview"));
                s.setAlbumCover((String) ((Map<?, ?>) map.get("album")).get("cover_medium"));
            }
        } catch (Exception e) {
            LOGGER.warn("No se pudo obtener meta de Deezer para ID {}: {}", id, e.getMessage());
        }

        if (s.getPreviewUrl() == null) s.setPreviewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
        if (s.getAlbumCover() == null) s.setAlbumCover("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjNDQ0Ii8+PC9zdmc+");
        
        playlistSongRepository.save(s);
    }

    /**
     * Devuelve la cola unificada optimizada para evitar N+1 queries.
     */
    public List<QueuedSong> getQueue() {
        // Obtenemos todos los datos necesarios en solo 3 consultas totales
        List<QueuedSong> paid = queuedSongRepository.findByHasPlayedFalseOrderByPositionAsc();
        List<PlaylistSong> allStatic = playlistSongRepository.findAll();
        PlaybackState state = getOrCreatePlaybackState();
        
        int currentIdx = state.getCurrentPlaylistIndex();
        int total = allStatic.size();
        
        List<QueuedSong> rotated = new ArrayList<>();
        if (total > 0) {
            // Creamos un mapa para acceso rápido por índice
            Map<Integer, PlaylistSong> songMap = allStatic.stream()
                .collect(Collectors.toMap(PlaylistSong::getPlaylistIndex, s -> s));

            for (int i = 0; i < total; i++) {
                int next = (currentIdx + i) % total;
                PlaylistSong ps = songMap.get(next);
                if (ps != null) {
                    rotated.add(convertPlaylistSongToQueued(ps));
                }
            }
        }
        
        List<QueuedSong> fullQueue = new ArrayList<>(paid);
        fullQueue.addAll(rotated);
        return fullQueue;
    }

    /**
     * Maneja el salto a la siguiente canción garantizando la consistencia del estado.
     */
    @Transactional
public Optional<QueuedSong> playNextSong() {
    PlaybackState state = getOrCreatePlaybackState();

    // 1. Prioridad: Canciones pagadas
    Optional<QueuedSong> paid = queuedSongRepository.findFirstByHasPlayedFalseOrderByPositionAsc();
    if (paid.isPresent()) {
        QueuedSong s = paid.get();
        queuedSongRepository.delete(s);
        state.setCurrentSongId(s.getSongId()); // Sincronizamos ID inmediatamente
        playbackStateRepository.save(state);
        return Optional.of(s);
    }

    // 2. Lista base (16 canciones)
    long total = playlistSongRepository.count();
    if (total == 0) return Optional.empty();

    // Calculamos el siguiente índice y actualizamos el estado ANTES de devolver la canción
    int nextIdx = (state.getCurrentPlaylistIndex() + 1) % (int) total;
    state.setCurrentPlaylistIndex(nextIdx);
    
    return playlistSongRepository.findByPlaylistIndex(nextIdx).map(ps -> {
        state.setCurrentSongId(ps.getSongId()); // Evita que el polling rebobine
        playbackStateRepository.save(state);
        LOGGER.info("Saltando a: {} (Índice {})", ps.getTitle(), nextIdx);
        return convertPlaylistSongToQueued(ps);
    });
}

    private QueuedSong convertPlaylistSongToQueued(PlaylistSong ps) {
        QueuedSong q = new QueuedSong();
        q.setId(ps.getId());
        q.setSongId(ps.getSongId());
        q.setTitle(ps.getTitle());
        q.setArtist(ps.getArtist());
        q.setAlbumCover(ps.getAlbumCover());
        q.setPreviewUrl(ps.getPreviewUrl());
        q.setDuration(ps.getDuration());
        return q;
    }

    private PlaybackState getOrCreatePlaybackState() {
        return playbackStateRepository.findById("global").orElseGet(() -> {
            PlaybackState n = new PlaybackState();
            n.setId("global");
            n.setCurrentPlaylistIndex(0);
            return n;
        });
    }

    public void addSongToQueue(String id, String t, String a, String c, String p, int d) {
        QueuedSong s = new QueuedSong();
        s.setSongId(id); s.setTitle(t); s.setArtist(a); s.setAlbumCover(c); s.setPreviewUrl(p);
        s.setPosition((int)queuedSongRepository.count() + 1);
        s.setHasPlayed(false);
        queuedSongRepository.save(s);
    }
}