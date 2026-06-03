package com.gramola.backend.repositories;

import com.gramola.backend.models.QueueItem;
import com.gramola.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de persistencia para la entidad QueueItem.
 */
public interface QueueRepository extends JpaRepository<QueueItem, Long> {
    
    List<QueueItem> findByBarOrderByPositionAsc(User bar);
    
    @Query("SELECT MAX(q.position) FROM QueueItem q WHERE q.bar = ?1")
    Optional<Integer> findMaxPositionByBar(User bar);
    
    @Query("SELECT q FROM QueueItem q WHERE q.bar = ?1 AND q.position > ?2")
    List<QueueItem> findItemsAfterPosition(User bar, int position);
}

