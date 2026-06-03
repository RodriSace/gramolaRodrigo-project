import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MusicService } from '../../services/music.service';

// PaymentSuccessComponent: Pantalla visual de paso de Angular que captura el éxito del cobro en Stripe Checkout.
// Es una pasarela circular:
// Stripe (Pago de tarjeta con éxito) -> Redirige al Frontend (PaymentSuccess) -> Comunica y actualiza la base de datos MySQL en Spring Boot.
@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="success-container">
        <div class="success-card">
            <div class="icon">✅</div>
            <h1>¡Pago Confirmado!</h1>
            <p>{{ displayMessage }}</p>
            <p class="redirect-msg">Redirigiendo a la Gramola...</p>
        </div>
    </div>
  `,
  styles: [`
    .success-container {
        height: 100vh;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #0d0d12;
        color: white;
        font-family: 'Inter', sans-serif;
    }
    .success-card {
        background: rgba(255,255,255,0.05);
        padding: 3rem;
        border-radius: 24px;
        text-align: center;
        border: 1px solid rgba(255,255,255,0.1);
    }
    .icon { font-size: 4rem; margin-bottom: 1rem; }
    h1 { color: #1db954; margin-bottom: 1rem; }
    .redirect-msg { margin-top: 2rem; color: #888; font-style: italic; }
  `]
})
export class PaymentSuccessComponent implements OnInit {
  displayMessage = 'Procesando tu pago...';

  constructor(
    private route: ActivatedRoute, // Inyectable para leer los parámetros queryParams del enlace de éxito de Stripe
    private musicService: MusicService, // Inyecta el servicio de música Angular
    private router: Router
  ) { }

  /**
   * AL CARGAR LA PANTALLA:
   * Leemos la barra de direcciones. Stripe Checkout nos devuelve a una de estas dos modalidades:
   * 
   * A) SI ES COBRO DE CANCIÓN (type=song):
   * Captura los detalles del tema y los inyecta mediante un POST en la base de datos de Spring Boot,
   * avisando a la pestaña principal a través del postMessage para recargar la cola en tiempo real.
   * 
   * B) SI ES COBRO DE SUSCRIPCIÓN MENSUAL (Token del Barman):
   * Llama a activar la suscripción del Barman en Spring Boot para permitirle iniciar sesión.
   */
  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const type = params['type'];

      // ========================================================
      // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASOS 3 Y 5
      // Captura el éxito del pago de Stripe y registra la canción premium en la cola (isPaid = true)
      // ========================================================
      if (type === 'song') {
        this.displayMessage = 'Añadiendo tu canción prioritaria a la cola de la Gramola...';

        // Creamos el payload capturando las variables codificadas que Stripe inyectó en la URL
        const payload = {
          trackId: params['trackId'],
          barId: params['barId'],
          title: params['title'],
          artist: params['artist'],
          previewUrl: params['previewUrl'],
          albumArtUrl: params['albumArtUrl'],
          durationMs: params['durationMs'] ? Number(params['durationMs']) : 180000,
          isPaid: true // Es un tema premium pagado con tarjeta (se cuela en pos 1)
        };

        // POST a Spring Boot para guardarla en la tabla 'playback_queue' de MySQL
        this.musicService.addToQueue(payload).subscribe({
          next: () => {
            this.displayMessage = '¡Pago completado y canción añadida con éxito! Cerrando ventana...';

            // COMUNICACIÓN ENTRE VENTANAS (window.opener.postMessage):
            // Como el cliente pagó desde una pestaña Popup flotante, enviamos un aviso invisible
            // a la ventana principal de la tablet del barman diciendo que el pago fue un éxito,
            // permitiéndole recargar la cola de MySQL en caliente al instante, y cerramos el Popup flotante.
            if (window.opener) {
              window.opener.postMessage({ type: 'payment-success', payload: payload }, '*');
              setTimeout(() => {
                window.close(); // Cierra el Popup flotante
              }, 1500);
            } else {
              setTimeout(() => this.router.navigate(['/music']), 3000);
            }
          },
          error: (err) => {
            console.error("Error al añadir a la cola", err);
            this.displayMessage = 'Error al registrar tu canción. Contacta con el bar.';
            if (window.opener) {
              setTimeout(() => window.close(), 3000);
            } else {
              setTimeout(() => this.router.navigate(['/music']), 3000);
            }
          }
        });
      }
      // ========================================================
      // 💳 FLUJO 3: EL BARMAN PAGA SU SUSCRIPCIÓN — RUTA B
      // Captura el token del Barman de Stripe y activa la suscripción en MySQL
      // ========================================================
      else {
        this.displayMessage = 'Tu suscripción se ha activado correctamente. Sincronizando con el servidor...';

        const token = params['token'];
        if (token) {
          // Llama al backend a /users/activate-subscription para poner 'subscriptionActive = true' en MySQL
          this.musicService.activateSubscription(token).subscribe({
            next: (res) => {
              console.log("Suscripción activada en el backend:", res);
              this.displayMessage = '¡Suscripción activada con éxito! Redirigiendo a la pantalla de inicio de sesión...';
              setTimeout(() => this.router.navigate(['/login']), 3000); // Manda al login para que entre ya activado
            },
            error: (err) => {
              console.error("Error al activar la suscripción en el backend:", err);
              this.displayMessage = 'Error de sincronización. Tu pago se ha realizado, pero no hemos podido activar la cuenta automáticamente. Por favor, contacta con soporte.';
              setTimeout(() => this.router.navigate(['/login']), 5000);
            }
          });
        } else {
          // Fallback por si acaso
          const userData = localStorage.getItem('user');
          if (userData) {
            const user = JSON.parse(userData);
            user.subscriptionActive = true;
            localStorage.setItem('user', JSON.stringify(user));
          }
          setTimeout(() => this.router.navigate(['/music']), 2000);
        }
      }
    });
  }


}
