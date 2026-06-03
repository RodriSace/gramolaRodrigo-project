package com.gramola.backend.services;

import com.gramola.backend.repositories.SystemConfigRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey; // Clave secreta privada de Stripe

    @Autowired
    private SystemConfigRepository configRepository; // Acceso a la configuración global de URLs

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey; // Inicialización de las credenciales de Stripe SDK
    }

    private String getFrontendUrl() {
        // Lee dinámicamente de base de datos la dirección URL de Angular
        return configRepository.findByKey("frontend_url")
                .map(c -> c.getValue())
                .orElse("http://127.0.0.1:4200");
    }

    // ========================================================
    // 💳 FLUJO 3: PAGO DE LA SUSCRIPCIÓN — PASO 2 (Stripe Service)
    // Inicializa la sesión con el SDK de Stripe en modo SUBSCRIPTION (mensual recurrente)
    // ========================================================
    /**
     * Crea una sesión de pago de Stripe Checkout para la suscripción mensual de un bar.
     */
    public String createSubscriptionSession(String customerEmail, String planName, long priceCents, String token) throws Exception {
        String frontendUrl = getFrontendUrl();
        
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD) 
            .setSuccessUrl(frontendUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}&token=" + token)
            .setCancelUrl(frontendUrl + "/payment-failed")
            .setCustomerEmail(customerEmail)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("eur")
                            .setUnitAmount(priceCents)
                            .setRecurring(
                                SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                    .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                    .build()
                            )
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Suscripción " + planName)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    // ========================================================
    // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 2 (Stripe Service)
    // Inicializa la sesión con el SDK de Stripe en modo PAYMENT (pago único)
    // ========================================================
    /**
     * Crea una sesión de pago de Stripe Checkout para el cobro de una canción solicitada.
     */
    public String createSongPaymentSession(String trackTitle, Long barId, String trackId, String artist, String previewUrl, String albumArtUrl, long durationMs, long priceCents) throws Exception {
        String frontendUrl = getFrontendUrl();
        
        String encodedTitle = java.net.URLEncoder.encode(trackTitle, "UTF-8");
        String encodedArtist = java.net.URLEncoder.encode(artist, "UTF-8");
        String encodedPreview = java.net.URLEncoder.encode(previewUrl != null ? previewUrl : "", "UTF-8");
        String encodedArt = java.net.URLEncoder.encode(albumArtUrl != null ? albumArtUrl : "", "UTF-8");
        
        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setSuccessUrl(frontendUrl + "/payment-success?type=song&trackId=" + trackId + "&barId=" + barId + "&title=" + encodedTitle + "&artist=" + encodedArtist + "&previewUrl=" + encodedPreview + "&albumArtUrl=" + encodedArt + "&durationMs=" + durationMs)
            .setCancelUrl(frontendUrl + "/music")
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("eur")
                            .setUnitAmount(priceCents)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Canción: " + trackTitle)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
