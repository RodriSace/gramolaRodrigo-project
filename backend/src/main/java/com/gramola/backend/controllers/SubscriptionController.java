package com.gramola.backend.controllers;

import com.gramola.backend.models.SubscriptionPlan;
import com.gramola.backend.models.User;
import com.gramola.backend.repositories.SubscriptionPlanRepository;
import com.gramola.backend.repositories.UserRepository;
import com.gramola.backend.services.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    @Autowired
    private SubscriptionPlanRepository planRepository; // Repositorio de planes de suscripción

    @Autowired
    private UserRepository userRepository; // Repositorio de usuarios/bares

    @Autowired
    private StripeService stripeService; // Servicio de pasarela de Stripe

    // ========================================================
    // 💳 FLUJO 3: PAGO DE LA SUSCRIPCIÓN — PASO 1 (Controller)
    // El controlador lee los planes de precios dinámicos de MySQL y los devuelve a Angular
    // Elemento: `ngOnInit()` hace GET a `/api/subscriptions/plans` -> llama a getPlans()
    // ========================================================
    /**
     * Obtiene la lista de planes de suscripción disponibles.
     */
    @GetMapping("/plans")
    public List<SubscriptionPlan> getPlans() {
        return planRepository.findAll(); // Devuelve las tarifas de MySQL
    }

    // ========================================================
    // 💳 FLUJO 3: PAGO DE LA SUSCRIPCIÓN — PASO 2 (Controller)
    // Recibe el plan y el token del barman, y llama a StripeService para iniciar el TPV recurrente
    // Elemento: `selectPlan()` en Angular hace POST a `/api/subscriptions/checkout` -> llama a checkout() / createCheckoutSession()
    // ========================================================
    /**
     * Crea una sesión de Stripe Checkout para la suscripción de un usuario.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(@RequestBody Map<String, String> data) {
        try {
            String token = data.get("token");
            Long planId = Long.valueOf(data.get("planId"));

            User user = userRepository.findByConfirmationToken(token)
                    .orElseThrow(() -> new RuntimeException("Token inválido o usuario no encontrado"));

            SubscriptionPlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

            // Genera el enlace seguro de redirección al Checkout de Stripe
            String url = stripeService.createSubscriptionSession(user.getEmail(), plan.getName(), plan.getPriceCents(), token);
            
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error al crear sesión de pago: " + e.getMessage()));
        }
    }
}
