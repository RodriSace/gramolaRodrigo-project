package com.example.gramolaRodrigo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.QueuedSong;

@Repository
public interface QueuedSongRepository extends JpaRepository<QueuedSong, Long> {

    /**
     * Busca todas las canciones que aún no han sido reproducidas.
     * Utilizado por QueueService.getQueue() para mostrar la lista en Angular.
     */
    List<QueuedSong> findByHasPlayedFalseOrderByPositionAsc();

    /**
     * Busca la canción que ocupa el primer lugar en la cola de espera.
     * Utilizado por QueueService.playNextSong() para decidir qué suena ahora.
     */
    Optional<QueuedSong> findFirstByHasPlayedFalseOrderByPositionAsc();

    /**
     * Busca una canción específica en la cola por su ID de Deezer.
     * Útil para evitar duplicados o verificar estados antes de procesar pagos.
     */
    Optional<QueuedSong> findBySongIdAndHasPlayedFalse(String songId);
}