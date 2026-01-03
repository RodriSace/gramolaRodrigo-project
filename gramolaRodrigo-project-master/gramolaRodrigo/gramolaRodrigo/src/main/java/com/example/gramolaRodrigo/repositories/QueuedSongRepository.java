package com.example.gramolaRodrigo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.QueuedSong;

@Repository
public interface QueuedSongRepository extends JpaRepository<QueuedSong, Long> {
	// Busca todas las canciones que no han sonado, ordenadas por ID
	List<QueuedSong> findByHasPlayedFalseOrderByIdAsc();

	// Busca solo la primera canción que no ha sonado, ordenada por ID
	Optional<QueuedSong> findFirstByHasPlayedFalseOrderByIdAsc();
}