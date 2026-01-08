package com.example.gramolaRodrigo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * Busca todas las suscripciones asociadas a un ID de bar.
     * Esencial para la lógica de limpieza en el registro de BarService.
     */
    List<Subscription> findByBarId(String barId);

    /**
     * Busca la suscripción activa más reciente de un bar basándose en la fecha de fin.
     * Útil para comprobar renovaciones o el estado actual del servicio.
     */
    Optional<Subscription> findTopByBarIdAndStatusOrderByEndAtDesc(String barId, String status);
    
    /**
     * Busca cualquier suscripción que coincida con un bar y un estado específico.
     * Utilizado para validar si el bar puede añadir canciones a la cola.
     */
    Optional<Subscription> findByBarIdAndStatus(String barId, String status);
}