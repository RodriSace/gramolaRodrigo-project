package com.gramola.backend.repositories;

import com.gramola.backend.models.BarSong;
import com.gramola.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositorio de persistencia para la entidad BarSong.
 */
public interface BarSongRepository extends JpaRepository<BarSong, Long> {
    
    List<BarSong> findByBarOrderByPlayedAtDesc(User bar);
}

