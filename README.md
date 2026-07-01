# 🎵 La Gramola Virtual

> Gramola digital interactiva para bares y locales: los clientes eligen la música, el local gestiona la cola y cobra por canción mediante **Stripe**.

Aplicación **full-stack** con frontend en **Angular 19** y backend en **Spring Boot 3** sobre **MySQL**. Cola musical persistente, control de clientes por tokens y pasarela de pago integrada.

## ✨ Características

- **Cola musical persistente** — las canciones solicitadas sobreviven a reinicios; ordenación configurable (FIFO, prioridad por pago).
- **Sistema de tokens** — cada cliente dispone de tokens para añadir canciones; el local decide el precio.
- **Pasarela de pago Stripe** — recarga de tokens con tarjeta directamente desde el móvil.
- **Panel del local** — gestión de la cola, salto de canciones y control de reproducción.
- **API REST documentada** — backend Spring Boot con arquitectura por capas (controller → service → repository).

## 🛠️ Stack

| Capa | Tecnología |
|---|---|
| Frontend | Angular 19, TypeScript, RxJS |
| Backend | Spring Boot 3, Java 17, JPA/Hibernate |
| Base de datos | MySQL 8 |
| Pagos | Stripe API |
| Pruebas de carga | JMeter |

## 🚀 Ejecución en local

### Backend
```bash
cd backend
./mvnw spring-boot:run
# API en http://localhost:8080
```

### Frontend
```bash
cd gramolafe
npm install
ng serve
# App en http://localhost:4200
```

> Configura las credenciales de MySQL y la clave de Stripe en `backend/src/main/resources/application.properties`.

## 📁 Estructura

```
├── backend/     # API REST Spring Boot (Java 17)
├── gramolafe/   # SPA Angular 19
└── jmeter/      # Planes de prueba de carga
```

## 👤 Autor

**Rodrigo de la Hoz López** — [Portfolio](https://rodrisace.github.io/mi-portfolio/) · [LinkedIn](https://www.linkedin.com/in/rodrigo-delahoz-db/)
