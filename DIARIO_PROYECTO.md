# Diario de Desarrollo: Proyecto Gramola Virtual
**Asignatura:** Tecnologías y Sistemas Web
**Arquitectura:** Angular 19 (Front) + Spring Boot 3 (Back) + MySQL

---

## [2026-05-12] Fase 1: Cimientos y Gestión de Usuarios

### 1. Análisis de Requisitos (Paso 0)
- Identificación de la diferencia clave: **La cola de reproducción se gestiona en el Backend**.
- Definición del flujo de datos: Registro -> Confirmación Mailtrap -> Pago Stripe -> Uso Gramola.
- Selección de tecnologías: Angular 19 Standalone, Spring Boot, MySQL, Stripe, Spotify API.

### 2. Diseño del Modelo de Datos (Paso 1)
- Creación de entidades JPA: `User`, `QueueItem`, `SubscriptionPlan`, `BarSong`.
- **Decisión Arquitectónica:** Usar una tabla `playback_queue` en MySQL para garantizar la persistencia de la cola ante reinicios del servidor.
- Configuración de `pom.xml` con dependencias de seguridad, correo y pagos.

### 3. Registro y Seguridad (Paso 2)
- Implementación de `BCrypt` para el hashing de contraseñas.
- Integración con **Mailtrap**: Envío de tokens de confirmación por email mediante `JavaMailSender`.
- Creación de endpoints de registro y confirmación de cuenta.

### 4. Suscripciones con Stripe (Paso 3)
- Integración del SDK de Stripe.
- Creación de **Checkout Sessions** para pagos de suscripción mensual/anual.
- Implementación de un **Webhook** para activar la suscripción del bar de forma asíncrona y segura.

## [2026-05-12] Fase 2: Lógica Musical y Spotify

### 5. Integración con Spotify (Paso 4)
- Implementación del **Client Credentials Flow** para obtener tokens de la API de Spotify sin requerir login del cliente final.
- Creación del servicio de búsqueda de canciones.

### 6. Lógica de Cola "Extraordinaria" (Paso 5)
- Implementación de la lógica de inserción: cuando un cliente paga, la canción se inserta en la **posición actual + 1**, desplazando el resto de la cola.
- **Decisión Arquitectónica:** Uso de `@Transactional` para asegurar que el desplazamiento de canciones y la inserción sean atómicos.
- Creación del `MusicController` para exponer la búsqueda y la gestión de la cola al frontend.

---
*Próximo hito: Inicio del desarrollo Frontend con Angular 19 Standalone.*

## [2026-05-12] Fase 3: Frontend con Angular 19 (Paso 6)

### 7. Inicialización del Proyecto (Standalone)
- Creación del proyecto `gramolafe` usando Angular CLI 19.
- Configuración de `provideHttpClient()` en `app.config.ts` para habilitar peticiones al backend.
- **Decisión Arquitectónica:** Uso de componentes standalone para eliminar la necesidad de `AppModule` y mejorar la modularidad.

### 8. Diseño de Interfaz y Experiencia de Usuario
- Implementación de un sistema de diseño basado en **Glassmorphism**.
- Creación de los componentes `Register` y `Login` con validaciones básicas y animaciones CSS.
- Uso de `localStorage` para la persistencia básica de la sesión del bar.

### 9. Servicios de Datos y Sincronización
- `QueueService`: Implementación de **Polling** mediante RxJS para mantener la cola del backend sincronizada en el cliente cada 5 segundos.
- **Decisión Arquitectónica:** Se prefiere Polling sobre WebSockets por simplicidad y facilidad de defensa, cumpliendo perfectamente con el requisito de "tiempo real".

---
*Próximo hito: Implementación del MusicComponent y flujo de pago de canciones.*

## [2026-05-12] Fase 4: Pruebas y Cierre (Pasos 9-11)

### 10. Prueba Funcional (Selenium)
- Implementación de un test automatizado en Java con Selenium WebDriver.
- El test simula la búsqueda, el pago y la redirección.
- **Validación Crítica:** La comprobación se realiza mediante una consulta SQL directa a la base de datos MySQL, asegurando que la canción se insertó correctamente en `playback_queue` tras el pago exitoso.

### 11. Prueba de Rendimiento (JMeter)
- Creación de un plan de pruebas `.jmx` para simular 1000 hilos concurrentes.
- El objetivo es medir la respuesta del servidor ante un pico de tráfico en el endpoint de login.

### 12. Documentación y Defensa
- Generación de este Diario de Proyecto.
- Inclusión de bloques **"PARA DEFENSA"** en todo el código fuente para justificar decisiones ante el tribunal.

---
**PROYECTO COMPLETADO.**
