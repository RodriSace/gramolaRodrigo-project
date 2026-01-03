package com.example.gramolaRodrigo.repositories;

import java.util.Optional; // Importa la clase Optional

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.Bar;

@Repository
public interface BarRepository extends JpaRepository<Bar, String> {

    // Spring Data JPA creará la consulta automáticamente a partir del nombre del método
    Optional<Bar> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByName(String name);

}