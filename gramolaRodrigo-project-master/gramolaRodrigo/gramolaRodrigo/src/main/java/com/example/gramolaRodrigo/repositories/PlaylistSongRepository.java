package com.example.gramolaRodrigo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gramolaRodrigo.entities.PlaylistSong;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, Long> {
    Optional<PlaylistSong> findByPlaylistIndex(int playlistIndex);
}