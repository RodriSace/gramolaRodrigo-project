package com.example.gramolaRodrigo.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "subscription_plans")
@Data
public class SubscriptionPlan {
    @Id
    private String id; // e.g. MONTHLY, ANNUAL

    private String name; // display name

    private long amountInCents; // price in cents

    private boolean active;
}
