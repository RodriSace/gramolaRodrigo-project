package com.example.gramolaRodrigo.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.entities.SongPrice;
import com.example.gramolaRodrigo.entities.SubscriptionPlan;
import com.example.gramolaRodrigo.repositories.SongPriceRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionPlanRepository;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/plans")
public class PlansController {

    private final SubscriptionPlanRepository planRepository;
    private final SongPriceRepository songPriceRepository;

    public PlansController(SubscriptionPlanRepository planRepository, SongPriceRepository songPriceRepository) {
        this.planRepository = planRepository;
        this.songPriceRepository = songPriceRepository;
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
        } else {
            // Asegurar que todos los planes existentes estén activos
            List<SubscriptionPlan> existingPlans = planRepository.findAll();
            for (SubscriptionPlan plan : existingPlans) {
                if (!plan.isActive()) {
                    plan.setActive(true);
                    planRepository.save(plan);
                    System.out.println(">>> Plan " + plan.getId() + " activado.");
                }
            }
        }

        // Inicializar precio de canción si no existe
        if (songPriceRepository.count() == 0) {
            SongPrice songPrice = new SongPrice();
            songPrice.setAmountInCents(99L); // 0.99€
            songPrice.setActive(true);
            songPriceRepository.save(songPrice);
            System.out.println(">>> Precio de canción inicializado por defecto.");
        } else {
            // Asegurar que haya al menos uno activo
            songPriceRepository.findFirstByActiveTrue().orElseGet(() -> {
                SongPrice songPrice = new SongPrice();
                songPrice.setAmountInCents(99L);
                songPrice.setActive(true);
                return songPriceRepository.save(songPrice);
            });
        }
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAllPlans() {
        List<SubscriptionPlan> plans = planRepository.findAll().stream()
            .filter(SubscriptionPlan::isActive)
            .toList();

        // Si no hay planes activos, inicializarlos
        if (plans.isEmpty()) {
            initPlans();
            plans = planRepository.findAll().stream()
                .filter(SubscriptionPlan::isActive)
                .toList();
        }

        return ResponseEntity.ok(plans);
    }

    @GetMapping("/song-price")
    public ResponseEntity<SongPrice> getSongPrice() {
        return songPriceRepository.findFirstByActiveTrue()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
