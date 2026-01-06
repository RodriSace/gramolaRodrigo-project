package com.example.gramolaRodrigo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.QueuedSong;

@Repository
public interface QueuedSongRepository extends JpaRepository<QueuedSong, Long> {
	// Busca todas las canciones que no han sonado, ordenadas por posición
	List<QueuedSong> findByHasPlayedFalseOrderByPositionAsc();

	// Busca solo la primera canción que no ha sonado, ordenada por posición
	Optional<QueuedSong> findFirstByHasPlayedFalseOrderByPositionAsc();

	// Busca por songId y hasPlayed false
	Optional<QueuedSong> findBySongIdAndHasPlayedFalse(String songId);
}
