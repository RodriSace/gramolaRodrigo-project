package com.gramola.backend.models;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad JPA vinculada a la tabla 'subscription_plans' que representa los planes de suscripción disponibles para los bares.
 */
@Entity
@Table(name = "subscription_plans")
@Data
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int priceCents;
    private int durationMonths;
    private String stripePriceId;
}

