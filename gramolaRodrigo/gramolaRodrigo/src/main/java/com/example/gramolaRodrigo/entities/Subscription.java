package com.example.gramolaRodrigo.entities;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "subscriptions")
@Data
public class Subscription {
    @Id
    private String id; // UUID

    private String barId;

    private String planId; // references SubscriptionPlan.id

    private Instant startAt;

    private Instant endAt;

    private String status; // ACTIVE, EXPIRED, CANCELED
}
