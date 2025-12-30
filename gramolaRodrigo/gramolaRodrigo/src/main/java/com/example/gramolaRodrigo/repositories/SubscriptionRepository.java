package com.example.gramolaRodrigo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
}
