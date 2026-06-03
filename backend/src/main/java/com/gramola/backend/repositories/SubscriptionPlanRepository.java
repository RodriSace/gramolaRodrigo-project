package com.gramola.backend.repositories;

import com.gramola.backend.models.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de persistencia para la entidad SubscriptionPlan.
 */
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
}

