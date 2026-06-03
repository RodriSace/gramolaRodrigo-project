package com.gramola.backend.repositories;

import com.gramola.backend.models.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio de persistencia para la entidad SystemConfig.
 */
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    Optional<SystemConfig> findByKey(String key);
}

