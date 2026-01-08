package com.example.gramolaRodrigo.entities;

import java.time.Instant;

import jakarta.persistence.Column; // Importa todo lo necesario de JPA
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "bar_id", nullable = false)
    private Bar bar;

    private String planId;
    private String status; // ACTIVE, EXPIRED, CANCELLED
    private Instant startAt;
    private Instant endAt;

    public Subscription() {}

    // GETTERS Y SETTERS MANUALES (Solución definitiva a "cannot find symbol")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Bar getBar() { return bar; }
    public void setBar(Bar bar) { this.bar = bar; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
}