package com.example.gramolaRodrigo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.gramolaRodrigo.entities.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    // Buscar la suscripción activa más reciente de un bar
    Optional<Subscription> findTopByBarIdAndStatusOrderByEndAtDesc(String barId, String status);
    
    // Opcional: Buscar cualquiera activa
    Optional<Subscription> findByBarIdAndStatus(String barId, String status);
}