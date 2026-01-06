package com.example.gramolaRodrigo.services;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.gramolaRodrigo.entities.PlaybackState;
import com.example.gramolaRodrigo.entities.PlaylistSong;
import com.example.gramolaRodrigo.entities.QueuedSong;
import com.example.gramolaRodrigo.repositories.PlaybackStateRepository;
import com.example.gramolaRodrigo.repositories.PlaylistSongRepository;
import com.example.gramolaRodrigo.repositories.QueuedSongRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class QueueService {

    private final QueuedSongRepository queuedSongRepository;
    private final PlaybackStateRepository playbackStateRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueService.class);

    public QueueService(QueuedSongRepository queuedSongRepository, 
                        PlaybackStateRepository playbackStateRepository, 
                        PlaylistSongRepository playlistSongRepository) {
        this.queuedSongRepository = queuedSongRepository;
        this.playbackStateRepository = playbackStateRepository;
        this.playlistSongRepository = playlistSongRepository;
    }

    @PostConstruct
    public void init() {
        // Limpieza básica
        playlistSongRepository.deleteAll();
        queuedSongRepository.deleteAll();
        
        // Carga de datos
        initializeRealDeezerPlaylist();
    }

    private void initializeRealDeezerPlaylist() {
        List<String> trackIds = List.of(
            "124895572", // Shape of You
            "809485762", // Blinding Lights
            "1420796",   // Viva la Vida
            "1109731"    // Levitating
        );

        int index = 0;
        for (String id : trackIds) {
            try {
                fetchFromDeezerAndSave(id, index++);
                Thread.sleep(100); 
            } catch (Exception e) {
                LOGGER.warn("Error cargando canción {}: {}", id, e.getMessage());
            }
        }
    }

    private void fetchFromDeezerAndSave(String id, int index) throws Exception {
        URL url = new URL("https://api.deezer.com/track/" + id);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        
        if (con.getResponseCode() != 200) return;

        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> trackData = mapper.readValue(con.getInputStream(), Map.class);
        
        if (trackData == null || trackData.containsKey("error")) return;

        PlaylistSong song = new PlaylistSong();
        song.setSongId(id);
        song.setTitle((String) trackData.get("title"));
        
        Map<?, ?> artistObj = (Map<?, ?>) trackData.get("artist");
        song.setArtist((String) artistObj.get("name"));
        
        Map<?, ?> albumObj = (Map<?, ?>) trackData.get("album");
        song.setAlbumCover((String) albumObj.get("cover_medium"));
        
        // NULL para usar el resolver
        song.setPreviewUrl(null); 
        song.setDuration(30); 
        song.setPlaylistIndex(index);
        
        playlistSongRepository.save(song);
    }

    // --- API Methods ---

    public List<QueuedSong> getQueue() {
        List<QueuedSong> paidQueue = queuedSongRepository.findByHasPlayedFalseOrderByPositionAsc();
        if (!paidQueue.isEmpty()) return paidQueue; 
        
        return List.of(getCurrentBackgroundSongAsQueued());
    }

    public Optional<QueuedSong> playNextSong() {
        Optional<QueuedSong> queueSong = queuedSongRepository.findFirstByHasPlayedFalseOrderByPositionAsc();
        if (queueSong.isPresent()) {
            QueuedSong song = queueSong.get();
            queuedSongRepository.delete(song); 
            return Optional.of(song);
        }
        return advanceToNextPlaylistSong().map(this::convertPlaylistSongToQueued);
    }

    // --- Helpers ---

    private QueuedSong getCurrentBackgroundSongAsQueued() {
        PlaybackState state = getOrCreatePlaybackState();
        int currentIndex = state.getCurrentPlaylistIndex();
        
        // Si la lista está vacía (falló internet), devolvemos una dummy para no dar error 500
        if (playlistSongRepository.count() == 0) {
            QueuedSong dummy = new QueuedSong();
            dummy.setSongId("0");
            dummy.setTitle("Cargando...");
            dummy.setArtist("Sistema");
            return dummy;
        }

        return playlistSongRepository.findByPlaylistIndex(currentIndex)
                .map(this::convertPlaylistSongToQueued)
                .orElseGet(() -> playlistSongRepository.findByPlaylistIndex(0)
                        .map(this::convertPlaylistSongToQueued).orElse(new QueuedSong())); 
    }

    private Optional<PlaylistSong> advanceToNextPlaylistSong() {
        PlaybackState state = getOrCreatePlaybackState();
        int currentIndex = state.getCurrentPlaylistIndex();
        int size = (int) playlistSongRepository.count();
        if (size == 0) return Optional.empty();

        int nextIndex = (currentIndex + 1) % size;
        state.setCurrentPlaylistIndex(nextIndex);
        state.setUpdatedAt(Instant.now());
        playbackStateRepository.save(state);
        
        return playlistSongRepository.findByPlaylistIndex(nextIndex);
    }

    private QueuedSong convertPlaylistSongToQueued(PlaylistSong ps) {
        QueuedSong q = new QueuedSong();
        if (ps == null) return q;
        q.setId(ps.getId());
        q.setSongId(ps.getSongId());
        q.setTitle(ps.getTitle());
        q.setArtist(ps.getArtist());
        q.setAlbumCover(ps.getAlbumCover());
        q.setPreviewUrl(ps.getPreviewUrl());
        q.setDuration(ps.getDuration());
        q.setPosition(0);
        return q;
    }

    private PlaybackState getOrCreatePlaybackState() {
        return playbackStateRepository.findById("global").orElseGet(() -> {
            PlaybackState n = new PlaybackState();
            n.setId("global");
            return n;
        });
    }

    public void addPaidSongToQueue(String songId, String title, String artist, String albumCover, String previewUrl, int duration) {
        QueuedSong s = new QueuedSong();
        s.setSongId(songId); s.setTitle(title); s.setArtist(artist);
        s.setAlbumCover(albumCover); s.setPreviewUrl(previewUrl); s.setDuration(duration);
        shiftPositionsFrom(1);
        s.setPosition(1);
        queuedSongRepository.save(s);
    }
    
    public void addSongToQueue(String songId, String title, String artist, String albumCover, String previewUrl, int duration) {
        QueuedSong s = new QueuedSong();
        s.setSongId(songId); s.setTitle(title); s.setArtist(artist);
        s.setAlbumCover(albumCover); s.setPreviewUrl(previewUrl); s.setDuration(duration);
        s.setPosition(getMaxPosition() + 1);
        queuedSongRepository.save(s);
    }

    public void savePlaybackPosition(String songId, double currentTimeSeconds, boolean paused) {
        PlaybackState ps = getOrCreatePlaybackState();
        ps.setCurrentSongId(songId);
        ps.setCurrentTimeSeconds(currentTimeSeconds);
        ps.setPaused(paused);
        playbackStateRepository.save(ps);
    }

    public Optional<PlaybackState> loadPlaybackState() {
        return playbackStateRepository.findById("global");
    }

    private int getMaxPosition() {
        List<QueuedSong> songs = queuedSongRepository.findByHasPlayedFalseOrderByPositionAsc();
        if (songs.isEmpty()) return 0;
        return songs.get(songs.size() - 1).getPosition();
    }

    private void shiftPositionsFrom(int startPosition) {
        List<QueuedSong> list = queuedSongRepository.findByHasPlayedFalseOrderByPositionAsc().stream()
            .filter(song -> song.getPosition() >= startPosition)
            .collect(Collectors.toList());
        for (QueuedSong song : list) {
            song.setPosition(song.getPosition() + 1);
            queuedSongRepository.save(song);
        }
    }
}