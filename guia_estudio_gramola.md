# 🎓 GUÍA DE ESTUDIO DEFINITIVA — LA GRAMOLA VIRTUAL
> **Para qué sirve esto:** Es tu guion completo para la demo. Léelo como si fuera un libreto de teatro: te dice qué hacer, qué decir, y qué significa cada cosa que pasa por detrás. Está escrito para alguien que **no sabe nada de código**.

---

# 📌 ÍNDICE

| Bloque | Contenido | ¿Cuándo lo necesitas? |
|--------|-----------|----------------------|
| **BLOQUE 0** | ¿Qué tecnologías usa y cómo funciona todo por dentro? | Cuando el profe pregunte "¿cómo está hecho?" |
| **BLOQUE 1** | Las 15 palabras técnicas que necesitas saber | Para no quedarte en blanco con ningún término |
| **BLOQUE 2** | Los 9 pasos de la demo explicados para tontos | **EL MÁS IMPORTANTE** — tu guion durante la demo |
| **BLOQUE 3** | Preguntas del tribunal + respuestas preparadas | Para las preguntas trampa que te hagan al final |
| **CHULETA** | Resumen de 1 minuto | Para repasar 5 min antes de entrar |

---
---

# 🟦 BLOQUE 0: ¿CÓMO ESTÁ HECHO ESTE PROYECTO?

> [!NOTE]
> Si el profesor te pregunta "¿qué tecnologías usas?" o "¿cómo está organizado?", esta es la sección que necesitas.

---

## 0.1 — ¿Qué es cada tecnología? (En cristiano)

Imagina que tu proyecto es un **restaurante**. Tiene 3 partes:

### 🍽️ LA SALA (Angular 19) — Lo que ve el cliente
Angular es un programa que crea **páginas web interactivas** (las pantallas con botones, formularios, colores). Está en la carpeta `gramolafe/`. Piensa en Angular como el **camarero y la decoración del restaurante**: es lo bonito que ve el cliente. Pero el camarero no cocina, solo apunta el pedido y lo lleva a la cocina.

**Dato clave para el profe:** Angular usa "componentes independientes" (Standalone Components). Cada pantalla es un archivo separado que funciona solo.

### 🍳 LA COCINA (Spring Boot 3 con Java) — El cerebro
Spring Boot es un programa Java que corre **en el servidor**. Está en la carpeta `backend/`. Es **la cocina del restaurante**: recibe los pedidos del camarero, aplica las reglas del negocio y le dice al almacén qué guardar. **El cliente NUNCA accede directamente a la base de datos**, siempre pasa por la cocina.

### 📦 EL ALMACÉN (MySQL) — La memoria permanente
MySQL guarda información en tablas (como hojas de Excel) en el disco duro. Es **el almacén frigorífico**: guarda los usuarios, las canciones y la configuración para que no se borre nada al apagar.

---

## 0.2 — ¿Cómo se comunican entre sí?

```
TÚ (pulsas botón) → ANGULAR (manda carta) → SPRING BOOT (cocina) → MYSQL (guarda) → respuesta de vuelta
```

Esa "carta" se llama **petición HTTP**: **GET** = "Dame datos" / **POST** = "Guárdame esto". Y viaja escrita en **JSON**: `{"bar": "La Gramola", "precio": 1.00}`

---

## 0.3 — El patrón Controlador → Servicio → Repositorio

> [!IMPORTANT]
> En el backend, cada petición pasa por 3 niveles en cadena:

| Nivel | Nombre | Rol en la cocina | Qué hace |
|-------|--------|------------------|----------|
| 1️⃣ | **Controlador** (`*Controller.java`) | El camarero | Recibe el pedido HTTP. No cocina, solo lo reparte al cocinero |
| 2️⃣ | **Servicio** (`*Service.java`) | El cocinero | Aplica la lógica: cifra claves, calcula precios, llama a Stripe/Spotify |
| 3️⃣ | **Repositorio** (`*Repository.java`) | El pinche | Va al almacén (MySQL) y saca o guarda datos |

---

## 0.4 — Las 5 tablas de la base de datos

| Tabla | Qué guarda | Columnas clave |
|-------|-----------|---------------|
| `users` | Ficha de cada bar | `email`, `password`, `confirmed`, `subscription_active`, `spotify_access_token`, `song_price_cents` |
| `playback_queue` | Cola de canciones | `title`, `artist`, `position` (0=sonando), `is_paid` (true=verde) |
| `bar_songs` | Historial de canciones cobradas | `title`, `artist`, `played_at` |
| `subscription_plans` | Planes de suscripción | `name`, `price_cents`, `duration_months` |
| `system_config` | URLs dinámicas del sistema | `key` ("frontend_url"), `value` ("http://localhost:4200") |

> Los precios y URLs **NO están a fuego en el código**. Se leen de estas tablas.

---

## 0.5 — Cómo arrancar el proyecto

1. **MySQL**: Abre XAMPP → Pulsa "Start" en MySQL
2. **Backend**: `mvn spring-boot:run` (esto ejecuta `DataInitializer.java` que rellena precios y URLs en MySQL)
3. **Frontend**: `ng serve` → Abrir `http://localhost:4200`

---
---

# 🟨 BLOQUE 1: LAS 15 PALABRAS QUE NECESITAS SABER

| # | Término | Qué es (para tu abuela) | Ejemplo real |
|---|---------|------------------------|-------------|
| 1 | **Petición HTTP** | Mandar una carta de Angular a Java | Pulsar "Registrarse" manda un POST |
| 2 | **GET / POST** | GET="dame datos" / POST="guárdame esto" | GET la cola, POST un registro |
| 3 | **JSON** | El idioma de las cartas: `{"bar":"Mi Bar"}` | Datos del formulario |
| 4 | **Endpoint** | La dirección de la carta: `/users/register` | Cada URL del backend |
| 5 | **Token UUID** | Código secreto aleatorio de usar y tirar | `"a3f5b2c1-7d8e..."` del email |
| 6 | **BCrypt** | Convierte `12345` en texto ilegible | Protege contraseñas en MySQL |
| 7 | **OAuth2** | Conectarse a Spotify sin saber su contraseña | El botón verde "Aceptar" |
| 8 | **Access Token** | Llave temporal de Spotify (1h) | Ticket de parking que caduca |
| 9 | **Refresh Token** | Llave permanente de Spotify | Abono anual del parking |
| 10 | **Polling (RxJS)** | Preguntar cada 1s: "¿Cambió la cola?" | Actualización en tiempo real |
| 11 | **Stripe Checkout** | Pantalla de Stripe donde pones tarjeta | Nosotros NUNCA vemos la tarjeta |
| 12 | **Webhook** | Aviso servidor→servidor si cierras el navegador | SMS automático de confirmación |
| 13 | **Singleton** | Solo UNA copia del reproductor en memoria | Evita duplicar música |
| 14 | **Autoplay Policy** | Chrome prohíbe música sin clic previo | Por eso pedimos un clic |
| 15 | **Debounce** | Ignorar señales duplicadas rápidas | Evita avanzar la cola 2 veces |

---
---

# 🟩 BLOQUE 2: LOS 9 PASOS DE LA DEMO (CON CÓDIGO INTEGRADO)

> [!IMPORTANT]
> Cada paso tiene: 🖱️ Qué haces → 📖 Historia con código incluido → 💬 Frase para el profe

---
---

## ✅ PASO 1 — REGISTRAR UNA CUENTA NUEVA

### 🖱️ Qué haces tú
Vas a `/register`, rellenas nombre del bar, email, contraseña (×2), Client ID y Client Secret de Spotify, y pulsas **"Registrarse"**.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular recoge los datos del formulario y los manda al servidor**

📂 **Clase:** `RegisterComponent` → `register.component.ts` (Angular)
📌 **Método:** `onRegister()`
```typescript
onRegister(event: Event) {
  event.preventDefault();  // Evita que la página se recargue sola

  // Empaqueta los datos del formulario en un JSON
  const info = {
    bar: this.barName,           // "La Gramola de Rodrigo"
    email: this.email,           // "rodrigo@email.com"
    pwd1: this.pwd1,             // "miClave123"
    pwd2: this.pwd2,             // "miClave123" (debe coincidir)
    clientId: this.clientId,     // ID de la app de Spotify del bar
    clientSecret: this.clientSecret  // Secreto de la app de Spotify
  };

  // Manda una carta POST al servidor con los datos
  this.userService.register(info).subscribe({
    next: () => {
      alert('Registro exitoso. Revisa tu email para confirmar.');
      this.router.navigate(['/login']);   // Redirige a la pantalla de login
    },
    error: (err) => {
      if (err.status === 409) alert('Error: El bar ya existe.');  // 409 = email duplicado
    }
  });
}
```

---

**2️⃣ El controlador recibe la carta y se la pasa al cocinero**

📂 **Clase:** `UserController.java` (Spring Boot)
📌 **Método:** `register()`
```java
@PostMapping("/register")
public ResponseEntity<Void> register(@RequestBody Map<String, String> body) {
    String bar = body.get("bar");       // Saca el nombre del JSON
    String email = body.get("email");   // Saca el email del JSON
    String pwd1 = body.get("pwd1");
    String pwd2 = body.get("pwd2");

    // Si las contraseñas no coinciden → error 406
    if (!pwd1.equals(pwd2)) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    // Le pasa todo al cocinero (UserService) para que haga el trabajo de verdad
    userService.register(bar, email, pwd1, clientId, clientSecret);
    return ResponseEntity.ok().build();   // Responde "200 OK" a Angular
}
```

---

**3️⃣ El cocinero aplica las reglas: borra fantasmas, cifra clave, genera token, guarda en MySQL**

📂 **Clase:** `UserService.java` (Spring Boot)
📌 **Método:** `register()`
```java
public void register(String barName, String email, String password, ...) throws Exception {
    // 1. Buscar si ya existe alguien con este email en MySQL
    Optional<User> existingUser = userRepository.findByEmail(email);

    if (existingUser.isPresent()) {
        User u = existingUser.get();
        // Si existe pero NO confirmó el email o NO pagó → borrar cuenta fantasma (Requisito 2.2)
        if (!u.isConfirmed() || !u.isSubscriptionActive()) {
            userRepository.delete(u);    // Borra esa fila de MySQL
            userRepository.flush();      // Fuerza el borrado AHORA
        } else {
            throw new Exception("El bar ya está registrado.");  // Error 409
        }
    }

    // 2. Crear la ficha nueva del bar
    User user = new User();
    user.setBarName(barName);              // Nombre del bar
    user.setEmail(email);                  // Email
    user.setPassword(password);            // Contraseña (cifrada con BCrypt)
    user.setConfirmationToken(UUID.randomUUID().toString());  // Código aleatorio: "a3f5-b2c1..."
    user.setSongPriceCents(100L);          // Precio por defecto: 100 céntimos = 1€

    // 3. Guardar en MySQL (INSERT INTO users ...)
    userRepository.save(user);

    // 4. Leer la URL del backend de MySQL (no hardcodeada)
    String backendUrl = configRepository.findByKey("backend_url").map(c -> c.getValue()).orElse("...");
    String confirmUrl = backendUrl + "/users/confirmToken/" + email + "?token=" + user.getConfirmationToken();

    // 5. Enviar email de confirmación por SMTP a Mailtrap
    mailService.sendEmail(email, "Confirma tu Gramola", "Haz clic aquí: " + confirmUrl);
}
```

---

**4️⃣ El cartero envía el email**

📂 **Clase:** `MailService.java` (Spring Boot)
📌 **Método:** `sendEmail()`
```java
public void sendEmail(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("no-reply@gramola.com");   // Remitente simulado
    message.setTo(to);                          // "rodrigo@email.com"
    message.setSubject(subject);                // "Confirma tu Gramola"
    message.setText(text);                      // El enlace con el token UUID
    mailSender.send(message);                   // Envía por SMTP a Mailtrap
}
```

---

### 💬 Frase para el profe
> *"Al pulsar Registrarse, Angular manda un POST a `/users/register`. El UserService primero comprueba si existe una cuenta fantasma con ese email y la limpia (Requisito 2.2). Luego cifra la contraseña con BCrypt, genera un token UUID, guarda todo en MySQL y envía un email de confirmación a Mailtrap con un enlace que contiene ese token."*

---
---

## ✅ PASO 2 — CONFIRMAR EL CORREO

### 🖱️ Qué haces tú
Abres **Mailtrap**, ves el email y pulsas el botón **"Confirmar Cuenta"**.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ El botón del email abre un enlace directo al backend**

El botón es un enlace tipo: `http://localhost:8080/users/confirmToken/rodrigo@email.com?token=a3f5...`
Va directamente a Spring Boot (puerto 8080), NO a Angular. Es una petición GET.

---

**2️⃣ El controlador recibe la petición, confirma la cuenta y redirige a Angular**

📂 **Clase:** `UserController.java` (Spring Boot)
📌 **Método:** `confirmToken()`
```java
@GetMapping("/confirmToken/{email}")
public void confirmToken(@PathVariable String email, @RequestParam String token,
                         HttpServletResponse response) throws IOException {
    // 1. Le dice al cocinero: "Confirma la cuenta de este token"
    boolean confirmed = userService.confirmAccount(token);

    if (confirmed) {
        // 2. Lee la URL de Angular desde MySQL (NO hardcodeada)
        String feUrl = configRepository.findByKey("frontend_url")
                .map(c -> c.getValue()).orElse("http://127.0.0.1:4200");

        // 3. Redirige el navegador a Angular → pantalla de pago
        response.sendRedirect(feUrl + "/payment?token=" + token);
        // El navegador se va a: http://localhost:4200/payment?token=a3f5...
    }
}
```

---

**3️⃣ El cocinero busca el token en MySQL y pone confirmed = true**

📂 **Clase:** `UserService.java` (Spring Boot)
📌 **Método:** `confirmAccount()`
```java
public boolean confirmAccount(String token) {
    return userRepository.findByConfirmationToken(token).map(user -> {
        user.setConfirmed(true);        // Cambia confirmed de false a true
        userRepository.save(user);       // UPDATE en MySQL
        return true;
    }).orElse(false);
}
```

---

### 💬 Frase para el profe
> *"El botón del email hace un GET directo al backend con el token UUID. El servicio lo busca en MySQL, pone confirmed a true, lee la URL del frontend de la tabla system_config para no hardcodearla, y redirige el navegador a la pantalla de pago de Angular."*

---
---

## ✅ PASO 3 — PAGAR LA SUSCRIPCIÓN CON STRIPE

### 🖱️ Qué haces tú
Estás en `/payment`. Ves 3 planes. Pulsas **"Elegir Plan"** en el Mensual. Se abre Stripe, pones la tarjeta de prueba `4242 4242 4242 4242`, pagas, y vuelves a la app.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular carga los planes de MySQL al abrir la pantalla**

📂 **Clase:** `SubscriptionComponent` → `subscription.component.ts` (Angular)
📌 **Método:** `loadPlans()`
```typescript
loadPlans() {
  // Manda un GET al servidor pidiendo los planes
  this.http.get<any[]>('http://127.0.0.1:8080/api/subscriptions/plans').subscribe({
    next: (data) => {
      this.plans = data;   // Angular pinta los 3 planes en pantalla
      // data = [{name: "Mensual", priceCents: 999}, {name: "Semestral", priceCents: 4999}, ...]
    }
  });
}
```

📂 **Clase:** `SubscriptionController.java` (Spring Boot)
📌 **Método:** `getPlans()`
```java
@GetMapping("/plans")
public List<SubscriptionPlan> getPlans() {
    return planRepository.findAll();  // Lee los planes de MySQL. NO hay precios en el código.
}
```

---

**2️⃣ Pulsas "Elegir Plan" → Angular manda un POST**

📂 **Clase:** `SubscriptionComponent` → `subscription.component.ts` (Angular)
📌 **Método:** `selectPlan()`
```typescript
selectPlan(planId: number) {
  this.http.post<any>('http://127.0.0.1:8080/api/subscriptions/checkout', {
    token: this.token,    // "a3f5-b2c1..." → identifica al barman
    planId: planId        // 1 → Plan Mensual
  }).subscribe({
    next: (res) => {
      if (res && res.url) {
        window.location.href = res.url;   // Redirige el navegador a Stripe
      }
    }
  });
}
```

---

**3️⃣ El controlador busca usuario y plan en MySQL, llama al cocinero de pagos**

📂 **Clase:** `SubscriptionController.java` (Spring Boot)
📌 **Método:** `createCheckoutSession()`
```java
@PostMapping("/checkout")
public ResponseEntity<?> createCheckoutSession(@RequestBody Map<String, String> data) {
    String token = data.get("token");
    Long planId = Long.valueOf(data.get("planId"));

    // Busca al barman en MySQL por su token
    User user = userRepository.findByConfirmationToken(token).orElseThrow(...);
    // Busca el plan en MySQL por su ID
    SubscriptionPlan plan = planRepository.findById(planId).orElseThrow(...);

    // Llama al cocinero de pagos con: email, nombre del plan, precio de MySQL, token
    String url = stripeService.createSubscriptionSession(
        user.getEmail(), plan.getName(), plan.getPriceCents(), token);

    return ResponseEntity.ok(Map.of("url", url));  // Devuelve la URL de Stripe a Angular
}
```

---

**4️⃣ StripeService crea la sesión de pago — LAS LÍNEAS ESTRELLA**

📂 **Clase:** `StripeService.java` (Spring Boot)
📌 **Método:** `createSubscriptionSession()`
```java
public String createSubscriptionSession(...) throws Exception {
    String frontendUrl = getFrontendUrl();  // Lee "http://localhost:4200" de MySQL

    SessionCreateParams params = SessionCreateParams.builder()
        // ⭐ ESTRELLA 1: Suscripción mensual recurrente (no pago único)
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        // ⭐ ESTRELLA 2: SOLO tarjeta bancaria (requisito del enunciado)
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        // URL de retorno si paga bien
        .setSuccessUrl(frontendUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}&token=" + token)
        .setCustomerEmail(customerEmail)
        // ⭐ ESTRELLA 3: Precio leído de MySQL (999 céntimos = 9,99€)
        .addLineItem(SessionCreateParams.LineItem.builder()
            .setQuantity(1L)
            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("eur")
                .setUnitAmount(priceCents)  // 999 → viene de MySQL, NO del código
                .setRecurring(...Interval.MONTH)
                .setProductData(...setName("Suscripción " + planName))
                .build())
            .build())
        .build();

    Session session = Session.create(params);  // Petición servidor→Stripe
    return session.getUrl();  // URL tipo https://checkout.stripe.com/pay/cs_test_xxx
}
```

---

**5️⃣ El usuario pone la tarjeta en Stripe y paga**

Nosotros **NUNCA vemos ni tocamos** el número de tarjeta. Stripe se encarga de todo (cumplimiento PCI).

---

**6️⃣ Stripe redirige de vuelta → Angular activa la suscripción**

📂 **Clase:** `PaymentSuccessComponent` → `payment-success.component.ts` (Angular)
📌 **Método:** `ngOnInit()`
```typescript
ngOnInit() {
  this.route.queryParams.subscribe(params => {
    const token = params['token'];  // Recoge el token de la URL
    if (token) {
      // Manda un POST: "Activa la suscripción de este barman"
      this.musicService.activateSubscription(token).subscribe({
        next: () => {
          setTimeout(() => this.router.navigate(['/login']), 3000);  // Redirige al login
        }
      });
    }
  });
}
```

📂 **Clase:** `UserService.java` (Spring Boot)
📌 **Método:** `activateSubscriptionByToken()`
```java
public void activateSubscriptionByToken(String token) {
    User user = userRepository.findByConfirmationToken(token).orElseThrow(...);
    user.setSubscriptionActive(true);   // Cambia de false a true
    userRepository.save(user);           // UPDATE en MySQL. Ya puede hacer login ✓
}
```

---

### 💬 Frase para el profe
> *"Los precios se leen de MySQL, no del código. El StripeService crea una sesión configurada como suscripción recurrente, restringida a tarjeta bancaria. Nosotros nunca tocamos la tarjeta. Cuando Stripe confirma, Angular activa la suscripción poniendo subscriptionActive a true en MySQL."*

---
---

## ✅ PASO 4 — RECUPERAR CONTRASEÑA

### 🖱️ Qué haces tú
En `/login`, pulsas **"¿Olvidaste tu contraseña?"**, metes tu email, abres Mailtrap, pulsas el enlace, escribes la nueva clave y pulsas **"Actualizar"**.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular manda el email al servidor**

📂 **Clase:** `LoginComponent` → `login.component.ts` (Angular)
📌 **Método:** `onForgotPassword()`
```typescript
onForgotPassword() {
  this.http.post('http://127.0.0.1:8080/users/forgot-password', {
    email: this.resetEmail   // El email que ha escrito el barman
  }).subscribe({
    next: () => { this.resetSuccessMessage = '¡Enlace enviado! Revisa Mailtrap.'; }
  });
}
```

---

**2️⃣ El cocinero genera un token de reset, lo guarda en MySQL y manda email**

📂 **Clase:** `UserService.java` (Spring Boot)
📌 **Método:** `generateResetToken()`
```java
public void generateResetToken(String email) throws Exception {
    User user = userRepository.findByEmail(email).orElseThrow(...);

    String token = UUID.randomUUID().toString();   // Código aleatorio de usar y tirar
    user.setResetPasswordToken(token);             // Lo guarda en la columna de MySQL
    userRepository.save(user);

    // Manda email con el enlace de recuperación
    String resetUrl = backendUrl + "/users/resetTokenRedirect/" + email + "?token=" + token;
    mailService.sendEmail(email, "Recuperación de contraseña", "Haz clic: " + resetUrl);
}
```

---

**3️⃣ El barman pulsa el enlace del email → el backend redirige a Angular**

📂 **Clase:** `UserController.java` (Spring Boot)
📌 **Método:** `resetTokenRedirect()`
```java
@GetMapping("/resetTokenRedirect/{email}")
public void resetTokenRedirect(...) {
    boolean valid = userService.isResetTokenValid(token);
    if (valid) {
        String feUrl = configRepository.findByKey("frontend_url")...;  // Lee URL de MySQL
        response.sendRedirect(feUrl + "/reset-password?token=" + token);
    }
}
```

---

**4️⃣ El barman escribe la nueva clave → el cocinero la cifra y borra el token**

📂 **Clase:** `UserService.java` (Spring Boot)
📌 **Método:** `resetPasswordWithToken()`
```java
public void resetPasswordWithToken(String token, String newPassword) throws Exception {
    User user = userRepository.findByResetPasswordToken(token).orElseThrow(...);
    user.setPassword(newPassword);          // Cifrada con BCrypt
    user.setResetPasswordToken(null);       // BORRA el token → el enlace caduca (uso único)
    userRepository.save(user);              // UPDATE en MySQL
}
```

---

### 💬 Frase para el profe
> *"Se genera un token UUID de un solo uso, se guarda en MySQL y se envía por email. Al confirmar la nueva contraseña, se cifra con BCrypt y se borra el token poniéndolo a null para que el enlace caduque automáticamente."*

---
---

## ✅ PASO 5 — LOGIN Y CONECTAR SPOTIFY (OAuth2)

### 🖱️ Qué haces tú
En `/login`, escribes email y contraseña, pulsas **"Entrar"**, te sale Spotify con el botón verde **"Aceptar"**, lo pulsas, y entras a `/music`.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular manda email y contraseña al servidor**

📂 **Clase:** `LoginComponent` → `login.component.ts` (Angular)
📌 **Método:** `onLogin()`
```typescript
onLogin(event: Event) {
  event.preventDefault();
  const credentials = { email: this.email, password: this.password };

  this.http.post('http://127.0.0.1:8080/users/login', credentials).subscribe({
    next: (user: any) => {
      // Guarda los datos del bar en localStorage (cajón del navegador)
      localStorage.setItem('user', JSON.stringify(user));

      if (user.spotifyConnected) {
        this.router.navigate(['/music']);   // Ya conectado → directo a la Gramola
      } else {
        this.getToken();  // Primera vez → va a Spotify a vincular
      }
    }
  });
}
```

---

**2️⃣ El cocinero verifica 3 cosas: clave + email confirmado + suscripción pagada**

📂 **Clase:** `UserController.java` (Spring Boot)
📌 **Método:** `login()`
```java
@PostMapping("/login")
public ResponseEntity<User> login(@RequestBody Map<String, String> credentials) {
    User user = userService.login(credentials.get("email"), credentials.get("password"));

    // 3 comprobaciones obligatorias:
    if (user != null && user.isConfirmed() && user.isSubscriptionActive()) {
        return ResponseEntity.ok(user);   // Login OK → devuelve los datos del bar
    } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();  // Denegado
    }
}
```

---

**3️⃣ Angular redirige a Spotify OAuth2 con `show_dialog=true`**

📂 **Clase:** `SpotiService.ts` (Angular) — Servicio Singleton `@Injectable`
📌 **Método:** `redirectToSpotify()`
```typescript
redirectToSpotify() {
  const scopes = ['streaming', 'user-read-playback-state', ...];  // Permisos necesarios
  const state = Math.random().toString(36).substring(7);  // Anti-falsificación (CSRF)

  // show_dialog=true OBLIGA a que salga SIEMPRE el botón verde "Aceptar"
  let params = `response_type=code&client_id=${this.clientId}&scope=${scopes}&show_dialog=true`;
  window.location.href = `https://accounts.spotify.com/authorize?${params}`;
  // El navegador se va a Spotify
}
```

---

**4️⃣ El barman pulsa "Aceptar" → Spotify redirige de vuelta con un código temporal**

Spotify redirige a: `/callback?code=AQBxyz...&state=abc`

---

**5️⃣ Angular recoge el código, lo limpia de la URL por seguridad, y lo manda al backend**

📂 **Clase:** `CallbackComponent` → `callback.component.ts` (Angular)
📌 **Método:** `ngOnInit()`
```typescript
ngOnInit(): void {
  const code = this.route.snapshot.queryParamMap.get('code');  // Saca el código de la URL

  // SEGURIDAD: Borra el código de la barra de direcciones para que nadie lo copie
  history.replaceState({}, '', '/callback');

  // Manda el código al backend para canjearlo por tokens definitivos
  this.spoti.getAuthorizationToken(code).subscribe({
    next: (data) => {
      this.spoti.spotiToken = data.access_token;  // Guarda la llave temporal
      this.router.navigateByUrl('/music');          // Va a la Gramola
    }
  });
}
```

---

**6️⃣ El cocinero canjea el código temporal por tokens definitivos con Spotify**

📂 **Clase:** `SpotiService.java` (Spring Boot)
📌 **Método:** `getAuthorizationToken()`
```java
public SpotiToken getAuthorizationToken(String code, String clientId, String email) {
    User user = userRepository.findByEmail(email).orElseThrow();

    // Prepara la petición HTTP a Spotify con Basic Auth (Base64)
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("grant_type", "authorization_code");

    String header = this.basicAuth(clientId, user.getSpotifyClientSecret());
    // Hace POST a https://accounts.spotify.com/api/token

    SpotiToken token = response.getBody();

    // Guarda las llaves en MySQL
    user.setSpotifyAccessToken(token.getAccessToken());   // Llave temporal (1h)
    user.setSpotifyRefreshToken(token.getRefreshToken());  // Llave permanente
    userRepository.save(user);   // UPDATE en MySQL
    return token;
}
```

---

### 💬 Frase para el profe
> *"El login valida credenciales, email confirmado y suscripción activa. Angular redirige a Spotify OAuth2 con show_dialog=true. Spotify devuelve un código temporal que el backend canjea por un Access Token de 1 hora y un Refresh Token permanente. Ambos se guardan en MySQL."*

---
---

## ✅ PASO 6 — SELECCIONAR UNA PLAYLIST

### 🖱️ Qué haces tú
En `/music`, haces clic sobre una playlist (ej: "Rock de los 80").

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular pide las canciones a Spotify y las manda al backend**

📂 **Clase:** `MusicComponent` → `music.component.ts` (Angular)
📌 **Método:** `selectPlaylist()`
```typescript
selectPlaylist(playlist: any) {
  // Pide las canciones de la playlist a la API de Spotify
  this.http.get(playlistUrl, { headers: { Authorization: `Bearer ${token}` } }).subscribe({
    next: (res: any) => {
      // Mapea las canciones a formato simple
      const tracksForBackend = tracks.map(t => ({
        title: t.name, artist: t.artists[0].name, trackId: t.id, ...
      }));

      // Manda las canciones al backend para guardarlas en MySQL
      this.http.post('http://127.0.0.1:8080/api/music/queue/load-playlist', {
        barId: this.barId,
        tracks: tracksForBackend
      }).subscribe(...);
    }
  });
}
```

---

**2️⃣ El cocinero aplica el algoritmo de limpieza selectiva**

📂 **Clase:** `QueueService.java` (Spring Boot)
📌 **Método:** `loadPlaylistIntoQueue()`
```java
public void loadPlaylistIntoQueue(User bar, List<Map<String, Object>> tracks) {
    // 1. Traer TODA la cola actual de MySQL
    List<QueueItem> allItems = queueRepository.findByBarOrderByPositionAsc(bar);

    // 2. Separar en dos grupos
    List<QueueItem> nonPaid = new ArrayList<>();   // Las grises (ambiente)
    List<QueueItem> paidSongs = new ArrayList<>();  // Las verdes (pagadas)
    for (QueueItem q : allItems) {
        if (q.isPaid()) paidSongs.add(q);
        else nonPaid.add(q);
    }

    // 3. Borrar SOLO las de ambiente. Las pagadas NO se tocan ✓
    queueRepository.deleteAll(nonPaid);
    queueRepository.flush();

    // 4. Reordenar las pagadas: pos 0, 1, 2...
    int nextPosition = 0;
    for (QueueItem paid : paidSongs) {
        paid.setPosition(nextPosition++);
        queueRepository.save(paid);
    }

    // 5. Insertar las nuevas al final de la cola
    for (Map<String, Object> track : tracks) {
        QueueItem item = new QueueItem();
        item.setTitle((String) track.get("title"));
        item.setPaid(false);                  // Canción de ambiente (gratis)
        item.setPosition(nextPosition++);     // Detrás de las pagadas
        queueRepository.save(item);           // INSERT en MySQL
    }
}
```

---

### 💬 Frase para el profe
> *"Al cambiar de playlist, QueueService aplica un algoritmo de limpieza selectiva: borra de MySQL solo las canciones ambientales, pero NUNCA toca las pagadas. Inserta las nuevas al final de la cola. Así protegemos la inversión del cliente."*

---
---

## ✅ PASO 7 — UN CLIENTE PAGA 1€ POR UNA CANCIÓN

### 🖱️ Qué haces tú
Un cliente busca "Despacito", pulsa **"Pagar canción"**, se abre un popup con Stripe, pone la tarjeta, paga, el popup se cierra solo, y "Despacito" aparece en verde.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular abre un popup (para no parar la música) y manda los datos al servidor**

📂 **Clase:** `MusicComponent` → `music.component.ts` (Angular)
📌 **Método:** `paySong()`
```typescript
paySong(track: any) {
  // Abre una ventana APARTE para que la música no pare durante el pago
  const paymentWindow = window.open('', 'StripePayment', `width=550,height=800`);
  // Escribe un spinner de carga en el popup
  paymentWindow.document.write(`<html>...Conectando con Stripe...</html>`);

  const payload = {
    barId: this.barId, title: track.name, trackId: track.id,
    artist: track.artists[0].name, durationMs: track.duration_ms
  };

  // Pide al servidor la URL de Stripe para este pago
  this.musicService.paySong(payload).subscribe({
    next: (res: any) => {
      paymentWindow.location.href = res.url;  // El popup se va a Stripe
    }
  });
}
```

---

**2️⃣ StripeService crea un pago ÚNICO (no suscripción) con precio dinámico**

📂 **Clase:** `StripeService.java` (Spring Boot)
📌 **Método:** `createSongPaymentSession()`
```java
// Misma estructura que la suscripción pero con 2 diferencias clave:
.setMode(SessionCreateParams.Mode.PAYMENT)   // ⭐ Pago ÚNICO (no recurrente)
.setUnitAmount(priceCents)                    // ⭐ Precio leído de song_price_cents de MySQL
// Cada bar puede cobrar un precio diferente por canción
```

---

**3️⃣ Stripe confirma → el popup recoge los datos y los manda al backend**

📂 **Clase:** `PaymentSuccessComponent` → `payment-success.component.ts` (Angular)
📌 **Método:** `ngOnInit()` — caso canción
```typescript
if (type === 'song') {
  const payload = {
    trackId: params['trackId'], barId: params['barId'],
    title: params['title'], artist: params['artist'],
    isPaid: true   // ¡Es una canción pagada! Se pone en verde
  };

  // POST al backend para añadirla a la cola
  this.musicService.addToQueue(payload).subscribe({
    next: () => {
      // Avisa a la ventana principal: "¡Ya pagué!"
      window.opener.postMessage({ type: 'payment-success', payload }, '*');
      setTimeout(() => window.close(), 1500);  // Cierra el popup
    }
  });
}
```

---

**4️⃣ EL ALGORITMO DE PRIORIDAD — Lo más importante del proyecto**

📂 **Clase:** `QueueService.java` (Spring Boot)
📌 **Método:** `addSongToQueue()`
```java
public void addSongToQueue(User bar, QueueItem newItem, boolean isPaid) {
    List<QueueItem> currentQueue = queueRepository.findByBarOrderByPositionAsc(bar);

    if (isPaid) {
        // Desplazar todas las canciones (de pos 1 en adelante) un puesto hacia abajo
        for (QueueItem item : currentQueue) {
            if (item.getPosition() >= 1) {
                item.setPosition(item.getPosition() + 1);  // Pos 1→2, 2→3, 3→4...
                queueRepository.save(item);
            }
        }
        newItem.setPosition(1);   // La canción pagada entra en POSICIÓN 1 (la siguiente en sonar)
    }

    newItem.setPaid(isPaid);
    queueRepository.save(newItem);  // INSERT en MySQL

    // Guardar en historial de facturación
    if (isPaid) {
        BarSong barSong = new BarSong();
        barSong.setTitle(newItem.getTitle());
        barSong.setBar(bar);
        barSongRepository.save(barSong);  // INSERT en tabla bar_songs
    }
}
```

**Visualización:**
```
ANTES:                        DESPUÉS:
Pos 0: "Bohemian" 🔊    →    Pos 0: "Bohemian" 🔊    (no se toca)
Pos 1: "Hotel Cal"       →    Pos 1: "DESPACITO" 💚   (¡SE CUELA!)
Pos 2: "Stairway"        →    Pos 2: "Hotel Cal"       (bajó de 1 a 2)
                               Pos 3: "Stairway"        (bajó de 2 a 3)
```

---

### 💬 Frase para el profe
> *"Abrimos un popup para no parar la música. El precio se lee de song_price_cents de MySQL. El algoritmo de prioridad en QueueService desplaza toda la cola +1 e inserta la canción pagada en posición 1. También la registra en bar_songs como historial de facturación."*

---
---

## ✅ PASO 8 — LA CANCIÓN TERMINA Y LA COLA AVANZA SOLA

### 🖱️ Qué haces tú
**Nada.** Es automático.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Angular detecta que la canción terminó y aplica Debounce**

📂 **Clase:** `MusicComponent` → `music.component.ts` (Angular)
📌 **Método:** `finishCurrentSong()`
```typescript
private isFinishing = false;  // Bandera de Debounce

private finishCurrentSong() {
  if (this.isFinishing) return;   // Si ya estamos procesando → IGNORAR señal duplicada
  this.isFinishing = true;        // Poner cartel de "Ocupado"

  // Avisa al servidor: "La canción de posición 0 ha terminado"
  this.http.post('http://127.0.0.1:8080/api/music/queue/finish', { barId: this.barId }).subscribe({
    next: () => {
      this.isFinishing = false;   // Quitar cartel de "Ocupado"
      // Refresca la cola y reproduce la nueva posición 0
    }
  });
}
```

---

**2️⃣ El cocinero borra la posición 0 y sube toda la cola -1**

📂 **Clase:** `QueueService.java` (Spring Boot)
📌 **Método:** `removeFirstAndAdvance()`
```java
public void removeFirstAndAdvance(User bar) {
    List<QueueItem> items = queueRepository.findByBarOrderByPositionAsc(bar);
    if (!items.isEmpty()) {
        // 1. Borrar la canción terminada (posición 0)
        queueRepository.delete(items.get(0));

        // 2. Restar -1 a todas las demás
        for (int i = 1; i < items.size(); i++) {
            QueueItem item = items.get(i);
            item.setPosition(item.getPosition() - 1);  // Pos 1→0, 2→1, 3→2...
            queueRepository.save(item);
        }
    }
    // La que estaba en pos 1 ahora es pos 0 → el Polling la detecta → se reproduce
}
```

**Visualización:**
```
ANTES (termina "Bohemian"):     DESPUÉS:
Pos 0: "Bohemian" 🔊      →    (BORRADA)
Pos 1: "Despacito" 💚      →    Pos 0: "Despacito" 💚 🔊  (sube a sonar)
Pos 2: "Hotel Cal"          →    Pos 1: "Hotel Cal"
```

---

### 💬 Frase para el profe
> *"Cuando Spotify detecta que terminó la canción, Angular usa un Debounce para evitar señales duplicadas. El backend borra la posición 0 de MySQL y resta -1 a todas las posiciones. El Polling detecta la nueva posición 0 y la reproduce automáticamente."*

---
---

## ✅ PASO 9 — CERRAR SESIÓN Y VOLVER A ENTRAR

### 🖱️ Qué haces tú
Pulsas **"Cerrar Sesión"**. Luego vuelves a hacer login y entras instantáneamente.

### 📖 La historia completa — CON CÓDIGO INCLUIDO

---

**1️⃣ Al cerrar sesión: pausa, limpia listeners, borra datos**

📂 **Clase:** `MusicComponent` → `music.component.ts` (Angular)
📌 **Método:** `logout()`
```typescript
logout() {
  if (this.sdkPlayer) {
    this.sdkPlayer.pause();                                    // Para la música
    this.sdkPlayer.removeListener('player_state_changed');     // Limpia vigilantes
    this.sdkPlayer.removeListener('ready');                    // (evita fugas de memoria)
    // ... limpia todos los listeners
  }
  localStorage.removeItem('user');        // Borra datos del bar del navegador
  localStorage.removeItem('spotiToken');  // Borra la llave de Spotify
  this.router.navigate(['/login']);        // Redirige al login
}
```

---

**2️⃣ Al re-loguearse: el Singleton reutiliza el reproductor existente**

📂 **Clase:** `MusicComponent` → `music.component.ts` (Angular)
📌 **Método:** `initSpotifySDK()`
```typescript
initSpotifySDK() {
  // ¿Ya existe un reproductor en memoria? (Singleton)
  if (this.spoti.sdkPlayer) {
    // SÍ existe → REUTILÍZALO (no crees otro)
    this.sdkPlayer = this.spoti.sdkPlayer;
    this.sdkDeviceId = this.spoti.sdkDeviceId;

    // Limpia listeners viejos del login anterior
    this.sdkPlayer.removeListener('player_state_changed');
    // Conecta listeners nuevos frescos
    this.registerPlayerListeners();
    return;   // NO crea otro reproductor
  }
  // Si NO existía (primera vez) → crear uno nuevo...
}
```

📂 **Clase:** `SpotiService.ts` (Angular) — **¿POR QUÉ funciona el Singleton?**
```typescript
@Injectable({ providedIn: 'root' })
// "providedIn: root" = Angular crea UNA SOLA COPIA de este servicio para TODA la app.
// Aunque cambies de pantalla (login → music → login → music), la copia es siempre la misma.
// Por eso this.spoti.sdkPlayer sigue existiendo al re-loguearse.
export class SpotiService {
  sdkPlayer: any = null;     // El reproductor vive aquí. Sobrevive a los cambios de pantalla.
  sdkDeviceId: string | null = null;
}
```

---

### 💬 Frase para el profe
> *"Al cerrar sesión, Angular pausa el reproductor y limpia todos los listeners para evitar fugas de memoria. Al re-loguearse, el Patrón Singleton reutiliza el reproductor existente del servicio @Injectable, evitando crear duplicados que causarían errores 404 en la API de Spotify."*

---
---

# 🟥 BLOQUE 3: PREGUNTAS DEL TRIBUNAL

> [!CAUTION]
> Memorízate estas respuestas.

### ❓ 1. *"¿Qué arquitectura usa el proyecto?"*
> *"Angular 19 como frontend con Standalone Components, Spring Boot 3 como backend con patrón Controlador-Servicio-Repositorio, y MySQL como base de datos. Comunicación mediante peticiones HTTP con datos en JSON."*

### ❓ 2. *"¿Cómo se cifran las contraseñas?"*
> *"Con BCrypt, un hash unidireccional: convierte la contraseña en texto ilegible y nunca se descifra. Al hacer login, BCrypt compara la clave escrita con la versión cifrada sin descifrarla."*

### ❓ 3. *"¿Por qué Polling y no WebSockets?"*
> *"Polling con RxJS es robusto y simple. Para una gramola donde las canciones duran minutos, un GET cada segundo es más que suficiente sin la sobrecarga de conexiones bidireccionales."*

### ❓ 4. *"¿Dónde se decide la prioridad de las canciones pagadas?"*
> *"En el backend, en QueueService.java, persistiendo en la tabla playback_queue de MySQL. Angular solo visualiza y reproduce la posición 0."*

### ❓ 5. *"¿Cómo cumplís el requisito de no hardcodear?"*
> *"Precios de suscripción en tabla subscription_plans, tarifa por canción en columna song_price_cents de users, y URLs en tabla system_config. Todo en MySQL."*

### ❓ 6. *"¿Por qué un popup para pagar la canción?"*
> *"Para no parar la música. Si redirigiéramos la pantalla principal, el reproductor de Spotify se detendría. El popup paga en una ventana aparte y usa postMessage para avisar."*

### ❓ 7. *"¿Qué es el Singleton y por qué lo usáis?"*
> *"El servicio SpotiService es @Injectable providedIn root: Angular crea una sola copia para toda la app. Si creáramos múltiples reproductores, Spotify daría errores 404."*

### ❓ 8. *"¿Qué pasa si pagan y cierran el navegador?"*
> *"Tenemos un Webhook de Stripe: un aviso servidor→servidor. El WebhookController recibe un POST de Stripe y activa la suscripción automáticamente."*

---
---

# 📋 CHULETA ULTRA-RÁPIDA

| # | Paso | Palabra clave |
|---|------|--------------|
| 1 | Registrarte | BCrypt, UUID, Mailtrap, borrado fantasmas Req 2.2 |
| 2 | Confirmar email | Token UUID, `confirmed=true`, URL de MySQL |
| 3 | Pagar suscripción | `SUBSCRIPTION`, `CARD`, precio de MySQL |
| 4 | Recuperar clave | UUID de un solo uso, BCrypt, token se borra |
| 5 | Login + Spotify | 3 validaciones, OAuth2, `show_dialog=true`, tokens en MySQL |
| 6 | Elegir playlist | Limpieza selectiva: borra grises, mantiene verdes |
| 7 | Cliente paga canción | Popup, precio dinámico, algoritmo prioridad pos 1 |
| 8 | Canción termina | Debounce, borra pos 0, cola sube -1 |
| 9 | Logout / Re-login | Limpia listeners, Singleton `@Injectable` |

> **¡Suerte! 🍀** Si te preguntan algo raro, tira de: *"El camarero (Controlador) recibe, el cocinero (Servicio) procesa, y el pinche (Repositorio) guarda en MySQL."*
