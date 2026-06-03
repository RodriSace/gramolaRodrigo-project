# 🎯 GUÍA RESUMEN — LA GRAMOLA (Solo lo importante)

> Esta guía va **al grano**. Cada paso te dice qué pasa, por dónde viaja el dato (clase → clase), y qué decirle al profe. Si quieres el código detallado, mira la guía completa.

---

# 🧠 LO QUE TIENES QUE SABER SÍ O SÍ

**3 capas del backend** (te lo van a preguntar):
```
Controlador (camarero) → Servicio (cocinero) → Repositorio (pinche del almacén → MySQL)
```
- El **Controlador** recibe la petición HTTP. No hace lógica, solo la pasa.
- El **Servicio** es donde está el cerebro: cifra contraseñas, crea pagos, valida cosas.
- El **Repositorio** solo habla con MySQL: guarda y busca datos.

**Angular → Spring Boot → MySQL → respuesta de vuelta.**
Siempre ese orden. Angular NUNCA toca MySQL directamente.

**5 tablas en MySQL**: `users`, `playback_queue`, `bar_songs`, `subscription_plans`, `system_config`

**Los precios y URLs NO están en el código.** Se leen de MySQL (`subscription_plans` y `system_config`).

---
---

# 📋 LOS 9 PASOS — FLUJOS RESUMIDOS

---

## ✅ PASO 1 — REGISTRO

**Qué haces:** Rellenas el formulario en `/register` y pulsas "Registrarse".

**Qué pasa por detrás:**
1. Angular empaqueta los datos en JSON y manda un **POST** a `/users/register`
2. El **Servicio** (UserService) comprueba si el email ya existe en MySQL:
   - Si existe pero nunca confirmó ni pagó → **la borra** (cuentas fantasma, Requisito 2.2)
   - Si existe y está activa → error 409
3. Crea el usuario con: contraseña cifrada (BCrypt) + token UUID aleatorio + precio canción = 1€
4. Guarda todo en MySQL
5. Envía un email a Mailtrap con un enlace que tiene el token UUID

**El flujo clase→clase:**
```
RegisterComponent (Angular)  →  POST /users/register
  → UserController.register()  →  UserService.register()
    → UserRepository.save()     [guarda en MySQL]
    → MailService.sendEmail()   [envía email por SMTP]
```

**💬 Frase:** *"Angular envía un POST. El servicio comprueba cuentas fantasma (Req 2.2), cifra con BCrypt, genera un token UUID, guarda en MySQL y envía email de confirmación por SMTP."*

---

## ✅ PASO 2 — CONFIRMAR EMAIL

**Qué haces:** Abres Mailtrap y pulsas "Confirmar Cuenta".

**Qué pasa por detrás:**
1. El botón del email va directo al **backend** (no a Angular): `GET /users/confirmToken/{email}?token=xxx`
2. El Servicio busca el token en MySQL → pone `confirmed = true`
3. Lee la URL de Angular de la tabla `system_config` (no hardcodeada)
4. Redirige el navegador a Angular: `/payment?token=xxx`

**El flujo clase→clase:**
```
Botón del email  →  GET /users/confirmToken/{email}
  → UserController.confirmToken()  →  UserService.confirmAccount()
    → UserRepository.save()   [confirmed = true en MySQL]
  → response.sendRedirect()  →  Angular /payment
```

**💬 Frase:** *"El botón hace un GET al backend. El servicio pone confirmed a true en MySQL, lee la URL del frontend de system_config y redirige a la pantalla de pago."*

---

## ✅ PASO 3 — PAGAR SUSCRIPCIÓN (Stripe)

**Qué haces:** En `/payment` eliges un plan y pagas con tarjeta `4242 4242 4242 4242`.

**Qué pasa por detrás:**
1. Angular carga los planes con un **GET** → los precios vienen de la tabla `subscription_plans` de MySQL
2. Al pulsar "Elegir Plan", Angular manda **POST** con tu token + ID del plan
3. El Controlador busca al usuario y al plan en MySQL, llama al StripeService
4. **StripeService** crea una sesión con 3 líneas estrella:
   - `Mode.SUBSCRIPTION` → pago mensual recurrente
   - `PaymentMethodType.CARD` → solo tarjeta (requisito del enunciado)
   - `priceCents` → precio leído de MySQL
5. Stripe devuelve una URL → Angular redirige ahí → el usuario paga
6. Stripe redirige de vuelta a `/payment-success` → Angular hace POST para activar
7. UserService pone `subscriptionActive = true` en MySQL

**El flujo clase→clase:**
```
SubscriptionComponent (Angular)  →  POST /api/subscriptions/checkout
  → SubscriptionController.createCheckoutSession()
    → Busca User y Plan en MySQL
    → StripeService.createSubscriptionSession()   [crea sesión en Stripe]
  → Angular redirige a Stripe  →  Usuario paga  →  Stripe redirige a /payment-success
PaymentSuccessComponent (Angular)  →  POST /users/activate-subscription
  → UserController.activate()  →  UserService.activateSubscriptionByToken()
    → UserRepository.save()   [subscriptionActive = true en MySQL]
```

**💬 Frase:** *"Los precios están en MySQL. StripeService crea una sesión como suscripción recurrente y solo tarjeta. Nosotros nunca tocamos la tarjeta (PCI). Al confirmar, se pone subscriptionActive a true."*

---

## ✅ PASO 4 — RECUPERAR CONTRASEÑA

**Qué haces:** En login pulsas "¿Olvidaste tu contraseña?", metes email, abres Mailtrap, pulsas el enlace, escribes nueva clave.

**Qué pasa por detrás:**
1. Angular manda **POST** `/users/forgot-password` con el email
2. UserService genera un **token UUID de un solo uso**, lo guarda en MySQL, envía email
3. El enlace del email va al backend → redirige a Angular `/reset-password?token=xxx`
4. Angular manda **POST** `/users/reset-password` con la nueva clave
5. UserService cifra la nueva clave con BCrypt y **borra el token** (poniéndolo a `null` → caduca el enlace)

**El flujo clase→clase:**
```
LoginComponent (Angular)  →  POST /users/forgot-password
  → UserController  →  UserService.generateResetToken()
    → Genera UUID, guarda en MySQL, envía email (MailService)

Botón del email  →  GET /users/resetTokenRedirect/{email}
  → UserController  →  redirige a Angular /reset-password

ResetPasswordComponent (Angular)  →  POST /users/reset-password
  → UserController  →  UserService.resetPasswordWithToken()
    → Cifra con BCrypt, pone token = null, guarda en MySQL
```

**💬 Frase:** *"Se genera un token UUID de un solo uso. Al confirmar, se cifra la nueva clave con BCrypt y se borra el token para que el enlace caduque automáticamente."*

---

## ✅ PASO 5 — LOGIN + CONECTAR SPOTIFY (OAuth2)

**Qué haces:** Escribes email/contraseña, pulsas "Entrar", aceptas en Spotify, entras a la Gramola.

**Qué pasa por detrás:**
1. Angular manda **POST** `/users/login` con email y contraseña
2. UserController verifica **3 cosas**: credenciales OK + `confirmed = true` + `subscriptionActive = true`
3. Angular guarda datos en `localStorage` y redirige a Spotify OAuth2 con **`show_dialog=true`** (fuerza que SIEMPRE salga el botón "Aceptar")
4. El barman pulsa "Aceptar" → Spotify redirige a `/callback?code=AQBxyz...`
5. CallbackComponent **limpia el código de la URL** (seguridad) con `history.replaceState()`
6. Manda el código al backend → SpotiService.java lo **canjea por 2 tokens** con Spotify:
   - **Access Token** = llave temporal (1 hora)
   - **Refresh Token** = llave permanente (para renovar sin re-login)
7. Guarda ambos tokens en MySQL → redirige a `/music`

**El flujo clase→clase:**
```
LoginComponent (Angular)  →  POST /users/login
  → UserController.login()  →  UserService.login()
    → Verifica: credenciales + confirmed + subscriptionActive

Angular  →  window.location.href = Spotify OAuth2 (show_dialog=true)
  → Spotify muestra "Aceptar"  →  redirige a /callback?code=xxx

CallbackComponent (Angular)  →  history.replaceState() [limpia URL]
  → GET /spoti/getAuthorizationToken?code=xxx
    → SpotiController  →  SpotiService.getAuthorizationToken()
      → POST a Spotify /api/token   [canjea código por tokens]
      → Guarda Access + Refresh Token en MySQL
  → Angular redirige a /music
```

**💬 Frase:** *"El login valida credenciales, email confirmado y suscripción activa. OAuth2 con show_dialog=true. Spotify devuelve un código que el backend canjea por Access Token (1h) y Refresh Token (permanente). Ambos en MySQL."*

---

## ✅ PASO 6 — ELEGIR PLAYLIST

**Qué haces:** Pulsas una playlist del panel izquierdo.

**Qué pasa por detrás:**
1. Angular pide las canciones a la API de Spotify y las manda al backend
2. **QueueService** aplica el **algoritmo de limpieza selectiva**:
   - Coge toda la cola de MySQL
   - **Borra SOLO las canciones de ambiente** (grises, no pagadas)
   - **Las pagadas (verdes) NO se tocan** → son sagradas
   - Inserta las canciones nuevas **al final**, detrás de las pagadas

**El flujo clase→clase:**
```
MusicComponent (Angular)  →  GET Spotify API [pide canciones]
  → POST /api/music/queue/load-playlist
    → MusicController  →  QueueService.loadPlaylistIntoQueue()
      → QueueRepository: borra grises, mantiene verdes, inserta nuevas al final
```

**💬 Frase:** *"QueueService aplica limpieza selectiva: borra solo las canciones de ambiente, nunca toca las pagadas. Inserta las nuevas al final. Así protegemos la inversión del cliente."*

---

## ✅ PASO 7 — CLIENTE PAGA POR UNA CANCIÓN (1€)

**Qué haces:** Un cliente busca una canción, pulsa "Pagar", paga en el popup, la canción aparece en verde en posición 1.

**Qué pasa por detrás:**
1. Angular abre un **popup** con `window.open()` → la música NO para (si redirigieras la pantalla principal, la música pararía)
2. Backend crea pago **ÚNICO** en Stripe: `Mode.PAYMENT` + precio de `song_price_cents` de MySQL (cada bar puede cobrar diferente)
3. Tras pagar, la pantalla de éxito manda los datos al backend con `isPaid = true`
4. **QueueService** ejecuta el **ALGORITMO DE PRIORIDAD**:
   - Posición 0 (la que suena) → **NO se toca**
   - Todas las demás (pos 1, 2, 3...) → **suben +1** (1→2, 2→3, 3→4)
   - La canción pagada se mete en **posición 1** (la siguiente en sonar)
5. También la registra en `bar_songs` (historial de facturación)
6. El popup usa `window.opener.postMessage()` para avisar a la ventana principal → cierra el popup con `window.close()`

**El flujo clase→clase:**
```
MusicComponent (Angular)  →  window.open() [popup]
  → POST /api/music/pay
    → MusicController  →  StripeService.createSongPaymentSession()
      → Mode.PAYMENT + precio de MySQL

Stripe confirma  →  PaymentSuccessComponent (popup)
  → POST /api/music/queue/add (isPaid=true)
    → MusicController  →  QueueService.addSongToQueue()
      → Desplaza cola +1, inserta en pos 1, guarda en bar_songs
  → window.opener.postMessage() + window.close()
```

**Visualización del algoritmo:**
```
ANTES:                        DESPUÉS:
Pos 0: "Bohemian" 🔊    →    Pos 0: "Bohemian" 🔊    (no se toca)
Pos 1: "Hotel Cal"       →    Pos 1: "DESPACITO" 💚   (¡SE CUELA!)
Pos 2: "Stairway"        →    Pos 2: "Hotel Cal"       (bajó)
                               Pos 3: "Stairway"        (bajó)
```

**💬 Frase:** *"Popup para no parar la música. Precio dinámico de MySQL. El algoritmo de prioridad desplaza la cola +1 e inserta la canción pagada en posición 1. Se registra en bar_songs para facturación."*

---

## ✅ PASO 8 — LA CANCIÓN TERMINA (AUTOMÁTICO)

**Qué haces:** Nada. Es automático.

**Qué pasa por detrás:**
1. El SDK de Spotify detecta que la canción acabó
2. Angular usa un **Debounce** (bandera `isFinishing`) para evitar procesar la señal 2 veces
3. Manda **POST** al backend: "La posición 0 ha terminado"
4. **QueueService** ejecuta `removeFirstAndAdvance()`:
   - **Borra** la posición 0 de MySQL
   - **Resta -1** a todas las demás (1→0, 2→1, 3→2)
   - La que estaba en pos 1 ahora es pos 0 → el **Polling** (cada 1 segundo) la detecta y la reproduce

**El flujo clase→clase:**
```
Spotify SDK detecta fin  →  MusicComponent.finishCurrentSong()
  → Debounce (isFinishing)
  → POST /api/music/queue/finish
    → MusicController  →  QueueService.removeFirstAndAdvance()
      → Borra pos 0, resta -1 a las demás en MySQL
  → Polling detecta nueva pos 0  →  playQueuePosition0()
```

**💬 Frase:** *"Spotify detecta el fin, Angular usa Debounce para evitar duplicados. El backend borra la posición 0 y sube toda la cola -1. El Polling detecta la nueva posición 0 y la reproduce."*

---

## ✅ PASO 9 — CERRAR SESIÓN Y RE-ENTRAR

**Qué haces:** Pulsas "Cerrar Sesión". Luego vuelves a hacer login.

**Qué pasa por detrás:**
1. Al cerrar: Angular **pausa** la música, **limpia los listeners** (evita fugas de memoria), **borra** localStorage
2. Al re-loguearse: **NO crea otro reproductor** → usa el **Patrón Singleton**
   - `SpotiService.ts` es `@Injectable({ providedIn: 'root' })` → Angular crea **UNA SOLA copia** para toda la app
   - El reproductor sobrevive al cambio de pantalla → se reutiliza

**El flujo clase→clase:**
```
MusicComponent.logout()
  → sdkPlayer.pause()  →  removeListener(...)  →  localStorage.clear()
  → Redirige a /login

MusicComponent.initSpotifySDK()  [al re-entrar]
  → ¿spoti.sdkPlayer ya existe? (Singleton)
    → SÍ → reutiliza el existente + registra listeners nuevos
    → NO → crea uno nuevo (primera vez)
```

**💬 Frase:** *"Al cerrar sesión se pausa y limpian listeners. Al re-entrar, el Singleton (@Injectable providedIn root) reutiliza el reproductor existente. Si creáramos duplicados, Spotify daría errores."*

---
---

# ⚡ CHULETA ULTRA-RÁPIDA

| Paso | Clave para el profe |
|------|-------------------|
| 1. Registro | BCrypt + UUID + borrado fantasmas (Req 2.2) + email Mailtrap |
| 2. Confirmar | Token UUID → `confirmed=true` + URL dinámica de MySQL |
| 3. Suscripción | `SUBSCRIPTION` + `CARD` + precio de MySQL + PCI |
| 4. Reset clave | UUID un solo uso → BCrypt + token se borra (null) |
| 5. Login+Spotify | 3 validaciones + OAuth2 `show_dialog=true` + Access/Refresh tokens |
| 6. Playlist | Limpieza selectiva: borra grises, mantiene verdes |
| 7. Pagar canción | Popup + precio dinámico + algoritmo prioridad pos 1 + bar_songs |
| 8. Fin canción | Debounce + borra pos 0 + cola sube -1 + Polling |
| 9. Logout | Limpia listeners + Singleton reutiliza reproductor |

> **Comodín universal:** *"El Controlador recibe, el Servicio procesa, el Repositorio guarda en MySQL."*
