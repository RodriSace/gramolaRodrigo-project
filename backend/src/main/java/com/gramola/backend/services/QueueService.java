package com.gramola.backend.services;

import com.gramola.backend.models.BarSong;
import com.gramola.backend.models.QueueItem;
import com.gramola.backend.models.User;
import com.gramola.backend.repositories.BarSongRepository;
import com.gramola.backend.repositories.QueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

@Service
@Transactional
public class QueueService {

    @Autowired
    private QueueRepository queueRepository; // Acceso a la tabla de cola de reproducción

    @Autowired
    private BarSongRepository barSongRepository; // Acceso al historial contable/reproducciones

    /**
     * Obtiene la cola de reproducción del bar ordenada por posición de forma ascendente.
     */
    public List<QueueItem> getQueueForBar(User bar) {
        return queueRepository.findByBarOrderByPositionAsc(bar);
    }

    // ========================================================
    // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 3 (Service Volcado)
    // Transaccional: vacía la cola ambiental no-pagada, compacta la pagada e inserta las nuevas canciones al final
    // ========================================================
    /**
     * Carga una playlist completa en la cola de reproducción del bar.
     * Mantiene las canciones pagadas por los clientes y reordena la cola.
     */
    public void loadPlaylistIntoQueue(User bar, List<Map<String, Object>> tracks) {
        System.out.println("[QUEUE] loadPlaylistIntoQueue - Recibidas " + tracks.size() + " canciones para bar: " + bar.getBarName());
        
        List<QueueItem> allItems = queueRepository.findByBarOrderByPositionAsc(bar);
        List<QueueItem> nonPaid = new ArrayList<>();
        List<QueueItem> paidSongs = new ArrayList<>();
        
        // Filtra canciones entre pagadas (prioritarias) y no pagadas (música ambiental)
        for (QueueItem q : allItems) {
            if (q.isPaid()) {
                paidSongs.add(q);
            } else {
                nonPaid.add(q);
            }
        }
        
        System.out.println("[QUEUE] Cola actual: " + allItems.size() + " total, " + paidSongs.size() + " pagadas, " + nonPaid.size() + " de fondo");
        
        // Purga las canciones ambientales anteriores de la base de datos
        if (!nonPaid.isEmpty()) {
            queueRepository.deleteAll(nonPaid);
            queueRepository.flush();
            System.out.println("[QUEUE] Borradas " + nonPaid.size() + " canciones de fondo antiguas");
        }

        // Reasigna posiciones correlativas a las pagadas para evitar huecos
        int nextPosition = 0;
        for (QueueItem paid : paidSongs) {
            paid.setPosition(nextPosition++);
            queueRepository.save(paid);
        }

        // Inserta los temas de la nueva playlist al final de la cola
        int insertCount = 0;
        for (Map<String, Object> track : tracks) {
            try {
                QueueItem item = new QueueItem();
                item.setBar(bar);
                item.setTitle((String) track.get("title"));
                item.setArtist((String) track.get("artist"));
                item.setSpotifyTrackId((String) track.get("trackId"));
                item.setAlbumArtUrl((String) track.get("albumArtUrl"));
                item.setDurationMs(track.get("durationMs") != null ? 
                    Long.valueOf(track.get("durationMs").toString()) : 0L);
                item.setPaid(false);
                item.setPosition(nextPosition++);
                item.setAddedAt(LocalDateTime.now());
                queueRepository.save(item);
                insertCount++;
            } catch (Exception e) {
                System.err.println("[QUEUE] ERROR insertando canción: " + track.get("title") + " -> " + e.getMessage());
            }
        }
        
        queueRepository.flush();
        System.out.println("[QUEUE] ✅ Insertadas " + insertCount + " canciones en la cola. Pos final: " + nextPosition);
    }

    // ========================================================
    // 🔄 FLUJO 6: CUANDO UNA CANCIÓN TERMINA — PASO 3 (Service)
    // Elimina la primera canción de MySQL (que ha terminado) y resta -1 a la posición del resto para avanzar
    // ========================================================
    /**
     * Elimina la primera canción de la cola (que acaba de terminar) y avanza las posiciones restantes.
     */
    @Transactional
    public void removeFirstAndAdvance(User bar) {
        List<QueueItem> items = queueRepository.findByBarOrderByPositionAsc(bar);
        if (!items.isEmpty()) {
            queueRepository.delete(items.get(0)); // Elimina la canción terminada
            
            // Resta -1 a todas las posiciones restantes para avanzar la cola
            for (int i = 1; i < items.size(); i++) {
                QueueItem item = items.get(i);
                item.setPosition(item.getPosition() - 1);
                queueRepository.save(item);
            }
        }
    }

    // ========================================================
    // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 4 (Service Cola)
    // Calcula la posición VIP (detrás de la última pagada), empuja las de detrás +1 y guarda en BarSong y QueueItem
    // ========================================================
    /**
     * Añade una canción a la cola. Si es pagada, se coloca después de las canciones
     * pagadas existentes (FIFO: la primera que se paga se oye primero).
     * Si no es pagada se añade al final de la cola.
     */
    public void addSongToQueue(User bar, QueueItem newItem, boolean isPaid) {
        newItem.setBar(bar);
        newItem.setAddedAt(LocalDateTime.now());
        newItem.setPaid(isPaid);
        
        List<QueueItem> currentQueue = queueRepository.findByBarOrderByPositionAsc(bar);

        if (currentQueue.isEmpty()) {
            newItem.setPosition(0);
        } else if (isPaid) {
            // Busca la posición de la ÚLTIMA canción pagada existente en la cola.
            // La nueva canción pagada se inserta DESPUÉS de ella (FIFO: la primera que se paga se oye primero).
            int posicionDeInsercion = 1; // Por defecto, justo después de la canción sonando (pos 0)

            for (QueueItem item : currentQueue) {
                if (item.isPaid() && item.getPosition() >= 1) {
                    posicionDeInsercion = item.getPosition() + 1;
                }
            }

            // Empuja hacia abajo solo las canciones desde la posición de inserción en adelante
            for (QueueItem item : currentQueue) {
                if (item.getPosition() >= posicionDeInsercion) {
                    item.setPosition(item.getPosition() + 1);
                    queueRepository.save(item);
                }
            }

            newItem.setPosition(posicionDeInsercion);
        } else {
            // Se coloca al final de la cola si es gratuita
            int lastPos = currentQueue.get(currentQueue.size() - 1).getPosition();
            newItem.setPosition(lastPos + 1);
        }

        queueRepository.save(newItem);
        
        // Registra el pago de la canción en el historial contable
        if (isPaid) {
            BarSong barSong = new BarSong();
            barSong.setBar(bar);
            barSong.setTitle(newItem.getTitle());
            barSong.setArtist(newItem.getArtist());
            barSong.setSpotifyTrackId(newItem.getSpotifyTrackId());
            barSong.setPlayedAt(LocalDateTime.now());
            barSongRepository.save(barSong);
        }
    }
}
