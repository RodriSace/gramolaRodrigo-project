import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

// SubscriptionComponent: Pantalla visual de Angular que muestra los planes de precios leídos de MySQL
@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './subscription.component.html',
  styleUrls: ['./subscription.component.css']
})
export class SubscriptionComponent implements OnInit {
  plans: any[] = [];
  token: string | null = null;
  loading = false;
  errorMessage = '';

  constructor(
    private http: HttpClient, // Cliente HTTP
    private route: ActivatedRoute, // Inyectable para poder capturar parámetros de la barra de direcciones (?token=xxx)
    private router: Router
  ) {}

  // ========================================================
  // 💳 FLUJO 3: PAGO DE LA SUSCRIPCIÓN — PASO 1
  // ngOnInit() hace GET a /api/subscriptions/plans para cargar planes dinámicos de MySQL
  // ========================================================
  /**
   * AL INICIAR LA PANTALLA:
   * 1. Captura el token UUID de confirmación que le pasamos desde la redirección del correo de Mailtrap.
   * 2. Llama a cargar los planes dinámicos desde la base de datos de Spring Boot.
   */
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      if (!this.token) {
        // Mensaje de advertencia si intenta acceder a la pantalla a mano sin el correo de confirmación
        this.errorMessage = 'No se ha detectado un token de confirmación. Por favor, accede a través del enlace de tu correo.';
      }
    });

    this.loadPlans(); // Llama a cargar las tarifas
  }

  /**
   * CARGAR TARIFAS DESDE MYSQL:
   * Pide los planes de pago dinámicos guardados en la tabla 'subscription_plans' de MySQL.
   * Cumple con la condición de no tener tarifas duras escritas en el frontend.
   */
  loadPlans() {
    this.http.get<any[]>('http://127.0.0.1:8080/api/subscriptions/plans').subscribe({
      next: (data) => {
        this.plans = data; // Guarda los planes en la variable y los dibuja en el HTML al instante
      },
      error: (err) => {
        console.error('Error al cargar los planes:', err);
        this.errorMessage = 'No se han podido cargar los planes de suscripción. Inténtalo de nuevo más tarde.';
      }
    });
  }

  // ========================================================
  // 💳 FLUJO 3: PAGO DE LA SUSCRIPCIÓN — PASO 2
  // selectPlan() hace POST a /api/subscriptions/checkout para crear la sesión de Stripe
  // ========================================================
  /**
   * COMPRAR EL PLAN SELECCIONADO (AL PULSAR 'SELECCIONAR PLAN'):
   * Hace un POST al backend pasándole el ID del plan elegido y el token UUID del barman.
   * Spring Boot responde con una URL de Stripe y Angular redirige al TPV seguro.
   */
  selectPlan(planId: number) {
    if (!this.token) {
      alert('Error: Token de confirmación ausente. Vuelve a hacer clic en el enlace de tu email.');
      return;
    }
    this.loading = true;

    this.http.post<any>('http://127.0.0.1:8080/api/subscriptions/checkout', {
      token: this.token,
      planId: planId
    }).subscribe({
      next: (res) => {
        // REDIRECCIÓN A STRIPE:
        // Si el backend nos devuelve una pasarela válida, redirige físicamente la pestaña activa
        // al TPV seguro oficial de los servidores de Stripe.
        if (res && res.url) {
          window.location.href = res.url;
        } else {
          this.errorMessage = 'No se recibió una URL válida de pago.';
          this.loading = false;
        }
      },
      error: (err) => {
        console.error('Error al iniciar checkout:', err);
        this.errorMessage = err.error?.message || 'Error al conectar con la pasarela de pago.';
        this.loading = false;
      }
    });
  }
}
