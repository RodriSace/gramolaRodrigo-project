package com.example.gramolaRodrigo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.entities.SubscriptionPlan;
import com.example.gramolaRodrigo.repositories.SubscriptionPlanRepository;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/plans")
public class PlansController {

    private final SubscriptionPlanRepository planRepository;

    public PlansController(SubscriptionPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // Inicializar planes si no existen al arrancar
    @PostConstruct
    public void initPlans() {
        if (planRepository.count() == 0) {
            SubscriptionPlan p1 = new SubscriptionPlan();
            p1.setId("MONTHLY");
            p1.setName("Plan Mensual");
            p1.setAmountInCents(999L); // 9.99€
            p1.setDurationDays(30);
            p1.setActive(true);
            planRepository.save(p1);

            SubscriptionPlan p2 = new SubscriptionPlan();
            p2.setId("ANNUAL");
            p2.setName("Plan Anual (Ahorro)");
            p2.setAmountInCents(9900L); // 99.00€
            p2.setDurationDays(365);
            p2.setActive(true);
            planRepository.save(p2);

            System.out.println(">>> Planes de suscripción inicializados por defecto.");
        }
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        return ResponseEntity.ok(planRepository.findAll().stream()
            .filter(SubscriptionPlan::isActive)
            .toList());
    }
}