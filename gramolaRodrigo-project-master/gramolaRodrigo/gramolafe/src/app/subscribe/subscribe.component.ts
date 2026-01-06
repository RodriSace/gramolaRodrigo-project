import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { API_URL } from '../api.config';
import { AuthService } from '../auth.service';

// Declaramos la variable global de Stripe
declare var Stripe: any;

@Component({
  selector: 'app-subscribe',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './subscribe.component.html',
  styles: [`
    /* Estilos específicos para el elemento de pago de Stripe */
    #payment-element {
      min-height: 200px;
      margin-bottom: 20px;
    }
  `]
})
export class SubscribeComponent implements OnInit {
  plans: any[] = [];
  message = '';
  loading = false; // Para mostrar spinners o deshabilitar botones
  
  // Variables de Stripe
  stripe: any;
  elements: any;
  clientSecret: string | null = null;
  
  // Estado del usuario
  isActive = false;
  selectedPlanId: string | null = null;

  constructor(
    private http: HttpClient, 
    private auth: AuthService, 
    private router: Router
  ) {}

  ngOnInit(): void {
    // 1. Inicializar Stripe con tu clave pública
    this.stripe = Stripe('pk_test_51SGRYtLYhxLLPwjrdBNI8h9ON82IcZ3vgt4lRa1rx9U9vMEyC9cLjeLvKB3IUHzzfUvoR55YvXHGNKhlrCIO5A7P00cvJSZ47X');

    // 2. Obtener planes del backend
    this.http.get<any[]>(`${API_URL}/plans`).subscribe({
      next: (d) => {
        console.log('Planes obtenidos del backend:', d);
        this.plans = d;
        // Si no hay planes en BD, creamos uno ficticio para que se vea algo en pantalla
        if (this.plans.length === 0) {
          console.log('No hay planes en BD, usando ficticios');
          this.plans = [
            { id: 'MONTHLY', name: 'Plan Mensual', amountInCents: 999 },
            { id: 'ANNUAL', name: 'Plan Anual (Ahorro)', amountInCents: 9900 }
          ];
        }
      },
      error: (err) => {
        console.error('Error cargando planes:', err);
        this.message = 'Error: No se pudieron cargar los planes. Usando planes de prueba.';
        // Mostrar planes ficticios en caso de error
        this.plans = [
          { id: 'MONTHLY', name: 'Plan Mensual', amountInCents: 999 },
          { id: 'ANNUAL', name: 'Plan Anual (Ahorro)', amountInCents: 9900 }
        ];
      }
    });

    // 3. Comprobar si ya está suscrito
    const bar = this.auth.currentUserValue;
    if (bar) {
      this.http.get<any>(`${API_URL}/bars/subscription-status`, { params: { barId: bar.id } }).subscribe(s => {
        this.isActive = !!s.active;
        if (this.isActive) {
          this.message = '¡Ya tienes una suscripción activa!';
          // Redirigir tras 2 segundos
          setTimeout(() => this.router.navigate(['/search']), 2000);
        }
      });
    } else {
      // Si no hay usuario, mandar al login
      this.router.navigate(['/login']);
    }
  }

  // Paso 1: El usuario elige un plan
  startCheckout(planId: string) {
    this.loading = true;
    this.message = '';
    this.selectedPlanId = planId;

    this.http.post(`${API_URL}/payments/subscription-intent`, { planId }).subscribe({
      next: (res: any) => {
        this.clientSecret = res.clientSecret;
        
        // Configuración visual de Stripe (Modo Oscuro para que cuadre con tu app)
        const appearance = { theme: 'night', labels: 'floating' };
        const options = { clientSecret: this.clientSecret, appearance };

        this.elements = this.stripe.elements(options);
        const paymentElement = this.elements.create('payment');

        // Esperar a que el DOM se actualice antes de montar
        setTimeout(() => {
          paymentElement.mount('#payment-element');
        }, 100);
        
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.message = 'Error al iniciar la pasarela de pago.';
        this.loading = false;
      }
    });
  }

  // Paso 2: Confirmar el pago con Stripe
  async confirm() {
    this.loading = true;
    this.message = '';

    const { error, paymentIntent } = await this.stripe.confirmPayment({
      elements: this.elements,
      redirect: 'if_required' // Importante para no recargar la página si no es necesario
    });

    if (error) {
      this.message = error.message || 'Error al procesar el pago.';
      this.loading = false;
      return;
    }

    if (paymentIntent && paymentIntent.status === 'succeeded') {
      this.finalizeSubscription();
    } else {
      this.message = 'El estado del pago es: ' + (paymentIntent ? paymentIntent.status : 'desconocido');
      this.loading = false;
    }
  }

  // Paso 3: Avisar al backend de que todo fue bien
  finalizeSubscription() {
    const bar = this.auth.currentUserValue;
    if (!bar || !this.selectedPlanId) return;

    this.http.post(`${API_URL}/payments/subscription-confirm`, { 
      planId: this.selectedPlanId, 
      barId: bar.id 
    }).subscribe({
      next: (res: any) => {
        this.message = '¡Suscripción activada correctamente!';
        this.isActive = true;
        this.clientSecret = null;
        this.loading = false;

        // Actualizar datos del usuario si el backend devuelve el objeto actualizado
        if (res?.bar) {
          sessionStorage.setItem('currentUser', JSON.stringify(res.bar));
        }

        // Redirigir a la app principal
        setTimeout(() => this.router.navigate(['/search']), 1500);
      },
      error: () => {
        this.message = 'Pago aceptado en Stripe, pero error al guardar en base de datos. Contacta con soporte.';
        this.loading = false;
      }
    });
  }
}