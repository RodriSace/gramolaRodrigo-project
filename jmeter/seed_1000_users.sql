-- ===========================================================
-- Script para crear 1000 usuarios de prueba para JMeter
-- Ejecutar ANTES de la prueba de rendimiento
-- ===========================================================
-- REQUISITO Sección 5: 1000 usuarios preexistentes se loguean
-- con sus credenciales correctas.
-- ===========================================================

-- Procedimiento para insertar 1000 usuarios
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS crear_usuarios_jmeter()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 1000 DO
        INSERT IGNORE INTO users (
            bar_name, 
            email, 
            password, 
            confirmed, 
            subscription_active, 
            song_price_cents,
            last_playlist_index
        ) VALUES (
            CONCAT('Bar JMeter ', i),
            CONCAT('user', i, '@gramola.com'),
            'password123',
            1,
            1,
            100,
            0
        );
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- Ejecutar el procedimiento
CALL crear_usuarios_jmeter();

-- Verificar cuántos se han creado
SELECT COUNT(*) AS total_usuarios_test FROM users WHERE email LIKE '%@gramola.com';

