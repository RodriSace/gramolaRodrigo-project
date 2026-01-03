package com.example.gramolaRodrigo.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.gramolaRodrigo.entities.PlaybackState;
import com.example.gramolaRodrigo.entities.QueuedSong;
import com.example.gramolaRodrigo.repositories.PlaybackStateRepository;
import com.example.gramolaRodrigo.repositories.QueuedSongRepository;

@Service
public class QueueService {

    private final QueuedSongRepository queuedSongRepository;
    private final PlaybackStateRepository playbackStateRepository;
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueService.class);

    // true = paused, false = playing
    private volatile boolean globallyPaused = false;

    @Autowired
    public QueueService(QueuedSongRepository queuedSongRepository, PlaybackStateRepository playbackStateRepository) {
        this.queuedSongRepository = queuedSongRepository;
        this.playbackStateRepository = playbackStateRepository;
        // initialize from persisted state if present
        Optional<PlaybackState> s = playbackStateRepository.findById("global");
        if (s.isPresent()) {
            PlaybackState ps = s.get();
            this.globallyPaused = ps.isPaused();
            LOGGER.info("Loaded persisted global pause state: paused={}", this.globallyPaused);
        }
    }

    public boolean isGloballyPaused() {
        return globallyPaused;
    }

    public boolean toggleGlobalPause() {
        globallyPaused = !globallyPaused;
        LOGGER.info("Global playback paused = {}", globallyPaused);
        // persist
        PlaybackState ps = playbackStateRepository.findById("global").orElseGet(() -> {
            PlaybackState n = new PlaybackState();
            n.setId("global");
            return n;
        });
        ps.setPaused(globallyPaused);
        ps.setUpdatedAt(Instant.now());
        playbackStateRepository.save(ps);
        return globallyPaused;
    }

    public void savePlaybackPosition(String songId, double currentTimeSeconds, boolean paused) {
        PlaybackState ps = playbackStateRepository.findById("global").orElseGet(() -> {
            PlaybackState n = new PlaybackState();
            n.setId("global");
            return n;
        });
        ps.setCurrentSongId(songId);
        ps.setCurrentTimeSeconds(currentTimeSeconds);
        ps.setPaused(paused);
        ps.setUpdatedAt(Instant.now());
        playbackStateRepository.save(ps);
        LOGGER.info("Saved playback position: song={} time={} paused={}", songId, currentTimeSeconds, paused);
    }

    public Optional<PlaybackState> loadPlaybackState() {
        return playbackStateRepository.findById("global");
    }

    public void addSongToQueue(String songId, String title, String artist, String albumCover, String previewUrl,
            int duration) {
        QueuedSong queuedSong = new QueuedSong();
        queuedSong.setSongId(songId);
        queuedSong.setTitle(title);
        queuedSong.setArtist(artist);
        queuedSong.setAlbumCover(albumCover);
        queuedSong.setPreviewUrl(previewUrl);
        queuedSong.setDuration(duration);

        queuedSongRepository.save(queuedSong);
        LOGGER.info("Added song to queue: {}", title);
    }

    public List<QueuedSong> getQueue() {
        return queuedSongRepository.findByHasPlayedFalseOrderByIdAsc();
    }

    public Optional<QueuedSong> playNextSong() {
        Optional<QueuedSong> nowPlayingOpt = queuedSongRepository.findFirstByHasPlayedFalseOrderByIdAsc();

        if (nowPlayingOpt.isPresent()) {
            QueuedSong nowPlaying = nowPlayingOpt.get();
            queuedSongRepository.delete(nowPlaying);
        }

        return queuedSongRepository.findFirstByHasPlayedFalseOrderByIdAsc();
    }
}