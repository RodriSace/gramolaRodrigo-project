package com.example.gramolaRodrigo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.gramolaRodrigo.services.QueueService;

@SpringBootApplication
public class GramolaRodrigoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GramolaRodrigoApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(QueueService queueService) {
        return args -> {
            // Ya no es necesario llamar manualmente a initializeDefaultPlaylist()
            // porque QueueService usa @PostConstruct para hacerlo al arrancar.
            System.out.println(">>> Gramola Rodrigo Backend Iniciado Correctamente!");
        };
    }
}