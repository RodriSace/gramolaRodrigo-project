package com.example.gramolaRodrigo;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.gramolaRodrigo.entities.SongPrice;
import com.example.gramolaRodrigo.entities.SubscriptionPlan;
import com.example.gramolaRodrigo.repositories.SongPriceRepository;
import com.example.gramolaRodrigo.repositories.SubscriptionPlanRepository;

@SpringBootApplication
public class GramolaRodrigoApplication {

	public static void main(String[] args) {
		// Para limpiar y recrear las tablas, ejecuta con el perfil "reset":
		//   -Dspring.profiles.active=reset
		// Esto usará application-reset.properties (ddl-auto=create-drop)
		SpringApplication.run(GramolaRodrigoApplication.class, args);
	}


	@Bean
	@SuppressWarnings("unused")
	ApplicationRunner seedData(SubscriptionPlanRepository planRepo, SongPriceRepository songPriceRepo) {
		return args -> {
			if (planRepo.findById("MONTHLY").isEmpty()) {
				SubscriptionPlan m = new SubscriptionPlan();
				m.setId("MONTHLY");
				m.setName("Mensual");
				m.setAmountInCents(999);
				m.setActive(true);
				planRepo.save(m);
			}
			if (planRepo.findById("ANNUAL").isEmpty()) {
				SubscriptionPlan a = new SubscriptionPlan();
				a.setId("ANNUAL");
				a.setName("Anual");
				a.setAmountInCents(9999);
				a.setActive(true);
				planRepo.save(a);
			}
			if (songPriceRepo.findFirstByActiveTrue().isEmpty()) {
				SongPrice p = new SongPrice();
				p.setAmountInCents(50);
				p.setActive(true);
				songPriceRepo.save(p);
			}
		};
	}

}
