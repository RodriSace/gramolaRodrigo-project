package com.example.gramolaRodrigo.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.gramolaRodrigo.entities.Bar;

@Repository
public interface BarRepository extends JpaRepository<Bar, String> {

    /**
     * Busca un bar por su dirección de correo electrónico.
     * Utilizado para el proceso de Login y para recuperar contraseñas.
     * @param email El correo del bar a buscar.
     * @return Un Optional que contiene el Bar si se encuentra.
     */
    Optional<Bar> findByEmail(String email);

    /**
     * Verifica si ya existe un registro con el email proporcionado.
     * @param email El correo a verificar.
     * @return true si el email ya está en la base de datos.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si ya existe un bar con el nombre proporcionado.
     * Utilizado en el registro para evitar nombres de bar duplicados.
     * @param name El nombre a verificar.
     * @return true si el nombre ya existe.
     */
    boolean existsByName(String name);

}