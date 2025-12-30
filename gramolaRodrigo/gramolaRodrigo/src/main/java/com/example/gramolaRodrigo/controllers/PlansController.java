package com.example.gramolaRodrigo.controllers;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.gramolaRodrigo.entities.SubscriptionPlan;
import com.example.gramolaRodrigo.repositories.SubscriptionPlanRepository;
import com.example.gramolaRodrigo.repositories.SongPriceRepository;

@RestController
@RequestMapping("/plans")
public class PlansController {

    private final SubscriptionPlanRepository planRepo;
    private final SongPriceRepository songPriceRepo;

    public PlansController(SubscriptionPlanRepository planRepo, SongPriceRepository songPriceRepo) {
        this.planRepo = planRepo;
        this.songPriceRepo = songPriceRepo;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> listActivePlans() {
        return ResponseEntity.ok(planRepo.findByActiveTrue());
    }

    @GetMapping("/song-price")
    public ResponseEntity<Map<String, Long>> getSongPrice() {
        long amount = songPriceRepo.findFirstByActiveTrue().map(p -> p.getAmountInCents()).orElse(50L);
        return ResponseEntity.ok(Map.of("amountInCents", amount));
    }
}
