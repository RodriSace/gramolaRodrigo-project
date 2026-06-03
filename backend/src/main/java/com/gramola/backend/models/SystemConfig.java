package com.gramola.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA vinculada a la tabla 'system_config' para almacenar configuraciones globales de tipo Clave-Valor.
 */
@Entity
@Table(name = "system_config")
@Getter
@Setter
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true)
    private String key;

    @Column(name = "config_value")
    private String value;
}

