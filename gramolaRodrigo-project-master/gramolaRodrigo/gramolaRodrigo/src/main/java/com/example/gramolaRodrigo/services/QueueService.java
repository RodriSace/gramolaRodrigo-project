package com.example.gramolaRodrigo.services;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            subscriptionRepository.deleteAll();
            barRepository.deleteAll();
            playlistSongRepository.deleteAll();
            queuedSongRepository.deleteAll();
            playbackStateRepository.deleteAll();

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

            initializeDynamicDeezerPlaylist();

            PlaybackState initialState = new PlaybackState();
            initialState.setId("global");
            initialState.setCurrentPlaylistIndex(0);
            playlistSongRepository.findByPlaylistIndex(0).ifPresent(s -> initialState.setCurrentSongId(s.getSongId()));
            playbackStateRepository.save(initialState);

            LOGGER.info(">>> Backend sincronizado: Datos de Deezer cargados.");
        } catch (Exception e) {
            LOGGER.error("Fallo crítico en init: {}", e.getMessage());
        }
    }

    private void initializeDynamicDeezerPlaylist() {
        List<String> trackIds = List.of("2947516331", "2801558052", "2728070371", "2610711672", "2743578151", "2954912511", "3050380851");
        int index = 0;
        for (String id : trackIds) { fetchAndSaveSong(id, index++); }
    }

    private void fetchAndSaveSong(String id, int idx) {
        PlaylistSong s = new PlaylistSong();
        s.setSongId(id); s.setPlaylistIndex(idx); s.setDuration(30);
        try {
            URL url = new URL("https://api.deezer.com/track/" + id);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (con.getResponseCode() == 200) {
                Map<?, ?> map = new ObjectMapper().readValue(con.getInputStream(), Map.class);
                Map<?, ?> artistMap = (Map<?, ?>) map.get("artist");
                Map<?, ?> albumMap = (Map<?, ?>) map.get("album");
                s.setTitle((String) map.get("title"));
                if (artistMap != null) s.setArtist((String) artistMap.get("name"));
                s.setPreviewUrl((String) map.get("preview"));
                if (albumMap != null) s.setAlbumCover((String) albumMap.get("cover_medium"));
                playlistSongRepository.save(s);
            }
        } catch (Exception e) {
            s.setTitle("Track #" + id); s.setArtist("Deezer Artist");
            s.setPreviewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3");
            playlistSongRepository.save(s);
        }
    }

    public List<QueuedSong> getQueue() {
        PlaybackState state = getOrCreatePlaybackState();
        String currentId = state.getCurrentSongId();
        int currentIdx = state.getCurrentPlaylistIndex();
        List<QueuedSong> fullQueue = new ArrayList<>();

        // 1. Identificar canción actual (priorizando pagos)
        List<QueuedSong> playedPaid = queuedSongRepository.findAll().stream()
                .filter(s -> s.getSongId().equals(currentId) && s.isHasPlayed())
                .collect(Collectors.toList());

        if (!playedPaid.isEmpty()) {
            fullQueue.add(playedPaid.get(playedPaid.size() - 1));
        } else {
            playlistSongRepository.findByPlaylistIndex(currentIdx)
                    .ifPresent(ps -> fullQueue.add(convertPlaylistSongToQueued(ps)));
        }

        // 2. Añadir canciones pagadas pendientes
        fullQueue.addAll(queuedSongRepository.findByHasPlayedFalseOrderByPositionAsc());

        // 3. Añadir resto de la playlist (circular)
        List<PlaylistSong> allStatic = playlistSongRepository.findAll();
        if (!allStatic.isEmpty()) {
            int total = allStatic.size();
            Map<Integer, PlaylistSong> songMap = allStatic.stream()
                    .collect(Collectors.toMap(PlaylistSong::getPlaylistIndex, s -> s));
            for (int i = 1; i < total; i++) {
                int nextIdx = (currentIdx + i) % total;
                Optional.ofNullable(songMap.get(nextIdx)).ifPresent(ps -> fullQueue.add(convertPlaylistSongToQueued(ps)));
            }
        }
        return fullQueue;
    }

    @Transactional
    public Optional<QueuedSong> playNextSong() {
        PlaybackState state = getOrCreatePlaybackState();
        // Prioridad: Pagadas
        Optional<QueuedSong> paid = queuedSongRepository.findFirstByHasPlayedFalseOrderByPositionAsc();
        if (paid.isPresent()) {
            QueuedSong s = paid.get();
            s.setHasPlayed(true);
            queuedSongRepository.save(s);
            state.setCurrentSongId(s.getSongId());
            playbackStateRepository.save(state);
            return Optional.of(s);
        }
        // Playlist normal
        long total = playlistSongRepository.count();
        if (total == 0) return Optional.empty();
        int nextIdx = (state.getCurrentPlaylistIndex() + 1) % (int) total;
        state.setCurrentPlaylistIndex(nextIdx);
        return playlistSongRepository.findByPlaylistIndex(nextIdx).map(ps -> {
            state.setCurrentSongId(ps.getSongId());
            playbackStateRepository.save(state);
            return convertPlaylistSongToQueued(ps);
        });
    }

    public void addSongToQueue(String id, String title, String artist, String cover, String preview, int duration) {
        QueuedSong s = new QueuedSong();
        s.setSongId(id);
        s.setTitle(Objects.requireNonNullElse(title, "Unknown Title"));
        s.setArtist(Objects.requireNonNullElse(artist, "Unknown Artist"));
        s.setAlbumCover(Objects.requireNonNullElse(cover, ""));
        s.setPreviewUrl(Objects.requireNonNullElse(preview, ""));
        s.setDuration(duration > 0 ? duration : 30);
        s.setPosition((int) queuedSongRepository.count() + 1);
        s.setHasPlayed(false);
        queuedSongRepository.save(s);
    }

    private QueuedSong convertPlaylistSongToQueued(PlaylistSong ps) {
        QueuedSong q = new QueuedSong();
        q.setSongId(ps.getSongId()); q.setTitle(ps.getTitle()); q.setArtist(ps.getArtist());
        q.setAlbumCover(ps.getAlbumCover()); q.setPreviewUrl(ps.getPreviewUrl()); q.setDuration(ps.getDuration());
        return q;
    }

    private PlaybackState getOrCreatePlaybackState() {
        return playbackStateRepository.findById("global").orElseGet(() -> {
            PlaybackState n = new PlaybackState(); n.setId("global"); n.setCurrentPlaylistIndex(0); return n;
        });
    }
}