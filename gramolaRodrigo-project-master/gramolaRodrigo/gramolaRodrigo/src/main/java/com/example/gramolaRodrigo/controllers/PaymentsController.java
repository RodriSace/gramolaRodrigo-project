package com.example.gramolaRodrigo.controllers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.gramolaRodrigo.entities.Bar;
import com.example.gramolaRodrigo.entities.Subscription;
import com.example.gramolaRodrigo.repositories.BarRepository;
import com.example.gramolaRodrigo.repositories.SongPriceRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionPlanRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionRepository;
import com.example.gramolaRodrigo.services.QueueService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

@RestController
@RequestMapping("/payments")
public class PaymentsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentsController.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    private final QueueService queueService;
    private final SongPriceRepository songPriceRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BarRepository barRepository;

    private final com.example.gramolaRodrigo.services.BarService barService;

    public PaymentsController(QueueService queueService, SongPriceRepository songPriceRepository,
            SubscriptionPlanRepository planRepository, SubscriptionRepository subscriptionRepository,
            BarRepository barRepository,
            com.example.gramolaRodrigo.services.BarService barService) {
        this.queueService = queueService;
        this.songPriceRepository = songPriceRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.barRepository = barRepository;
        this.barService = barService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    // Crear PaymentIntent para suscripción según planId
    @PostMapping("/subscription-intent")
    public ResponseEntity<Map<String, String>> createSubscriptionIntent(@RequestBody Map<String, Object> body) {
        Object planIdObj = body.get("planId");
        if (planIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_planId"));
        }
        String planId = planIdObj.toString();
        long amount = planRepository.findById(planId).map(p -> p.getAmountInCents()).orElse(0L);
        if (amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_plan"));
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("eur")
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();
        try {
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));
        } catch (StripeException e) {
            LOGGER.error("Error creando payment intent de suscripción", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Confirmación de suscripción (persistimos registro simple)
    @PostMapping("/subscription-confirm")
    public ResponseEntity<Map<String, Object>> confirmSubscription(@RequestBody Map<String, Object> body) {
        Object planIdObj = body.get("planId");
        Object barIdObj = body.get("barId");
        if (planIdObj == null || barIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_fields"));
        }
        String planId = planIdObj.toString();
        String barId = barIdObj.toString();
        if (planRepository.findById(planId).isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_plan"));
        }

        // Validar que el bar exista; si no, devolvemos error claro
        java.util.Optional<Bar> barOpt = barRepository.findById(barId);
        if (barOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "bar_not_found"));
        }

        // Marcar el bar como verificado al completar la suscripción (mejor UX)
        Bar bar = barOpt.get();
        if (!bar.isVerified()) {
            bar.setVerified(true);
            bar.setVerifiedAt(Instant.now());
            barRepository.save(bar);
        }

        Subscription s = new Subscription();
        s.setId(java.util.UUID.randomUUID().toString());
        s.setBarId(barId);
        s.setPlanId(planId);
        s.setStartAt(Instant.now());
        // Periodo a modo de ejemplo: 30 días para mensual, 365 para anual
        if ("MONTHLY".equals(planId)) {
            s.setEndAt(Instant.now().plus(Duration.ofDays(30)));
        } else if ("ANNUAL".equals(planId)) {
            s.setEndAt(Instant.now().plus(Duration.ofDays(365)));
        }
        s.setStatus("ACTIVE");
        subscriptionRepository.save(s);
        return ResponseEntity.ok(new java.util.HashMap<>() {{
            put("status", "subscription_active");
            put("bar", bar);
        }});
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@RequestBody Map<String, Object> data) {
        // Precio por canción desde BD
        long amount = songPriceRepository.findFirstByActiveTrue()
            .map(p -> p.getAmountInCents())
            .orElse(50L);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amount)
            .setCurrency("eur")
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();
        try {
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return ResponseEntity.ok(Map.of("clientSecret", paymentIntent.getClientSecret()));
        } catch (StripeException e) {
            LOGGER.error("Error creando payment intent de canción", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Confirmación de pago de canción: añade a la cola
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(@RequestBody Map<String, Object> payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "payload_missing"));
        }

        Object songIdObj = payload.get("songId");
        Object titleObj = payload.get("title");
        Object artistObj = payload.get("artist");
        Object albumCoverObj = payload.get("albumCover");
        Object previewUrlObj = payload.get("previewUrl");
        Object durationObj = payload.get("duration");

        Object barIdObj = payload.get("barId");
        if (barIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_barId"));
        }
        String barId = barIdObj.toString();
        if (!barService.hasActiveSubscription(barId)) {
            return ResponseEntity.status(402).body(Map.of("error", "subscription_required"));
        }

        if (songIdObj == null || titleObj == null || artistObj == null || albumCoverObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "missing_song_fields"));
        }

        String songId = songIdObj.toString();
        String title = titleObj.toString();
        String artist = artistObj.toString();
        String albumCover = albumCoverObj.toString();
        String previewUrl = previewUrlObj != null ? previewUrlObj.toString() : null;
        int duration = 30; // valor por defecto
        if (durationObj != null) {
            try {
                duration = Integer.parseInt(durationObj.toString());
            } catch (NumberFormatException nfe) {
                return ResponseEntity.badRequest().body(Map.of("error", "invalid_duration"));
            }
        }

    LOGGER.info(">>> PAGO CONFIRMADO EN BACKEND para la canción: {}", title);
        try {
            queueService.addSongToQueue(songId, title, artist, albumCover, previewUrl, duration);
            return ResponseEntity.ok(Map.of("status", "song_queued"));
        } catch (Exception e) {
            LOGGER.error("Error al añadir la canción a la cola", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "failed_to_queue_song", "message", e.getMessage()));
        }
    }
}

