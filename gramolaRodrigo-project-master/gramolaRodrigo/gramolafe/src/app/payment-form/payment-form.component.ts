import { Component, EventEmitter, Input, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';
import { AuthService } from '../auth.service';

// Declaramos la variable global de Stripe
declare var Stripe: any;

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-form.component.html',
  styles: [`
    .modal-overlay {
      position: fixed; top: 0; left: 0; width: 100%; height: 100%;
      background: rgba(0,0,0,0.8); z-index: 2000;
      display: flex; justify-content: center; align-items: center;
      backdrop-filter: blur(5px);
    }
    .payment-card {
      width: 90%; max-width: 400px;
      background: rgba(30, 30, 30, 0.95);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 16px;
      padding: 2rem;
      box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
      animation: slideUp 0.3s ease;
      color: white;
    }
    .song-info { color: #bb86fc; margin-bottom: 0.5rem; font-size: 1.1rem; }
    .price { font-size: 1.2rem; margin-bottom: 1.5rem; color: #03dac6; font-weight: bold; }
    #payment-element {
      min-height: 200px;
      margin-bottom: 20px;
    }
    .actions { display: flex; justify-content: space-between; margin-top: 1.5rem; gap: 1rem; }
    .btn-pay {
      background: linear-gradient(45deg, #bb86fc, #7c4dff); color: white; border: none;
      padding: 10px 20px; border-radius: 50px; cursor: pointer; font-weight: bold; width: 100%;
    }
    .btn-cancel {
      background: transparent; border: 1px solid #cf6679; color: #cf6679;
      padding: 10px 20px; border-radius: 50px; cursor: pointer; font-weight: bold; width: 100%;
    }
    .loading { opacity: 0.6; pointer-events: none; }
    @keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
  `]
})
export class PaymentFormComponent implements OnInit, OnDestroy {
  @Input() songTitle: string = '';
  @Input() price: number = 0.99;
  @Input() songData: any = null;
  @Output() cancel = new EventEmitter<void>();
  @Output() success = new EventEmitter<void>();

  // Variables de Stripe
  stripe: any;
  elements: any;
  clientSecret: string | null = null;
  loading = false;
  message = '';

  // Variables para toast
  showSuccess = false;
  successMessage = '';



  constructor(private http: HttpClient, private auth: AuthService) {}

  ngOnInit(): void {
    // Prevenir scroll del body
    document.body.classList.add('modal-open');

    // Inicializar Stripe con la clave pública
    this.initializeStripe();
  }

  private initializeStripe() {
    // Verificar que Stripe esté cargado
    if (typeof Stripe === 'undefined') {
      console.error('Stripe no está cargado');
      this.message = 'Error: Stripe no está disponible. Recarga la página.';
      return;
    }

    try {
      this.stripe = Stripe('pk_test_51SGRYtLYhxLLPwjrdBNI8h9ON82IcZ3vgt4lRa1rx9U9vMEyC9cLjeLvKB3IUHzzfUvoR55YvXHGNKhlrCIO5A7P00cvJSZ47X');
      // Crear PaymentIntent para la canción
      this.createPaymentIntent();
    } catch (error) {
      console.error('Error inicializando Stripe:', error);
      this.message = 'Error al inicializar la pasarela de pago.';
    }
  }

  ngOnDestroy(): void {
    // Restaurar scroll del body
    document.body.classList.remove('modal-open');

    // Limpiar elementos de Stripe
    if (this.elements) {
      this.elements = null;
    }
  }

  createPaymentIntent() {
    this.loading = true;
    this.message = '';

    // Enviar datos de la canción para calcular el precio correcto
    const payload = this.songData ? {
      songId: this.songData.id,
      title: this.songData.title
    } : {};

    this.http.post(`${API_URL}/payments/create-payment-intent`, payload).subscribe({
      next: (res: any) => {
        if (!res || !res.clientSecret) {
          console.error('Respuesta inválida del servidor:', res);
          this.message = 'Error: Respuesta inválida del servidor.';
          this.loading = false;
          return;
        }

        this.clientSecret = res.clientSecret;
        console.log('PaymentIntent creado:', this.clientSecret);

        // Configuración visual de Stripe (Modo Oscuro)
        const appearance = { theme: 'night', labels: 'floating' };
        const options = {
          clientSecret: this.clientSecret,
          appearance
        };

        try {
          this.elements = this.stripe.elements(options);
          const paymentElement = this.elements.create('card', {
            style: {
              base: {
                color: '#ffffff',
                fontFamily: '"Helvetica Neue", Helvetica, sans-serif',
                fontSmoothing: 'antialiased',
                fontSize: '16px',
                '::placeholder': {
                  color: '#aab7c4'
                }
              },
              invalid: {
                color: '#fa755a',
                iconColor: '#fa755a'
              }
            },
            hidePostalCode: true
          });

          // Esperar a que el DOM se actualice antes de montar
          setTimeout(() => {
            try {
              paymentElement.mount('#payment-element');
              console.log('Stripe PaymentElement montado correctamente');
              this.loading = false;
            } catch (mountError) {
              console.error('Error montando PaymentElement:', mountError);
              this.message = 'Error al cargar el formulario de pago.';
              this.loading = false;
            }
          }, 100);
        } catch (elementError) {
          console.error('Error creando elementos de Stripe:', elementError);
          this.message = 'Error al inicializar el formulario de pago.';
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('Error creando payment intent:', err);
        this.message = err.error?.error || 'Error al iniciar la pasarela de pago.';
        this.loading = false;
      }
    });
  }

  onCancel() {
    this.cancel.emit();
  }

  async onPay() {
    if (!this.stripe || !this.elements) return;

    this.loading = true;
    this.message = '';

    const cardElement = this.elements.getElement('card');

    const { error, paymentIntent } = await this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: {
        card: cardElement,
        billing_details: {
          name: 'Cliente' // Puedes obtener esto del formulario si es necesario
        }
      },
      return_url: 'http://localhost:4200/payment-success' // URL de retorno para evitar nueva pestaña
    });

    if (error) {
      this.message = error.message || 'Error al procesar el pago.';
      this.loading = false;
      return;
    }

    if (paymentIntent && paymentIntent.status === 'succeeded') {
      this.confirmPayment();
    } else {
      this.message = 'El estado del pago es: ' + (paymentIntent ? paymentIntent.status : 'desconocido');
      this.loading = false;
    }
  }

  confirmPayment() {
    const bar = this.auth.currentUserValue;
    if (!bar || !this.songData) {
      console.error('Datos faltantes para confirmar pago:', { bar, songData: this.songData });
      this.message = 'Error: Datos de usuario o canción faltantes.';
      this.loading = false;
      return;
    }

    const payload = {
      songId: this.songData.id,
      title: this.songData.title,
      artist: this.songData.artist || 'Desconocido',
      albumCover: this.songData.albumCover || '',
      previewUrl: this.songData.preview,
      duration: this.songData.duration || 30,
      barId: bar.id
    };

    console.log('Enviando confirmación de pago:', payload);

    this.http.post(`${API_URL}/payments/confirm`, payload).subscribe({
      next: (res: any) => {
        console.log('Pago confirmado exitosamente');
        this.loading = false;

        // Mostrar toast de éxito
        this.successMessage = `¡Pago aceptado! "${this.songData.title}" añadida a la cola.`;
        this.showSuccess = true;

        // Ocultar toast después de 4 segundos
        setTimeout(() => {
          this.showSuccess = false;
        }, 4000);

        // Cerrar modal después de mostrar toast
        setTimeout(() => {
          this.success.emit();
        }, 500);
      },
      error: (err) => {
        console.error('Error confirmando pago:', err);
        const errorMsg = err.error?.error || 'Error desconocido';
        this.message = `Pago aceptado en Stripe, pero error al añadir a la cola: ${errorMsg}`;
        this.loading = false;

        // Si es error de suscripción, mostrar mensaje específico
        if (errorMsg.includes('subscription_required')) {
          this.message = 'Error: Necesitas una suscripción activa para añadir canciones.';
        }
      }
    });
  }
}
