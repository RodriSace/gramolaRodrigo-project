package com.gramola.backend.config;

import com.gramola.backend.models.SubscriptionPlan;
import com.gramola.backend.models.SystemConfig;
import com.gramola.backend.repositories.SubscriptionPlanRepository;
import com.gramola.backend.repositories.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos de la base de datos al arrancar la aplicación.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private SystemConfigRepository configRepository;

    @Override
    public void run(String... args) throws Exception {
        if (planRepository.count() == 0) {
            System.out.println("====== INICIALIZADOR: Insertando planes de suscripción oficiales en MySQL ======");
            savePlan("Plan Mensual", 999, 1, "price_monthly_id");
            savePlan("Plan Semestral", 4999, 6, "price_six_months_id");
            savePlan("Plan Anual", 8999, 12, "price_yearly_id");
        }

        seedConfig("frontend_url", "http://127.0.0.1:4200");
        seedConfig("backend_url", "http://127.0.0.1:8080");
    }

    private void savePlan(String name, int price, int months, String stripeId) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName(name);
        plan.setPriceCents(price);
        plan.setDurationMonths(months);
        plan.setStripePriceId(stripeId);
        planRepository.save(plan);
    }

    private void seedConfig(String key, String value) {
        if (configRepository.findByKey(key).isEmpty()) {
            SystemConfig config = new SystemConfig();
            config.setKey(key);
            config.setValue(value);
            configRepository.save(config);
        }
    }
}

