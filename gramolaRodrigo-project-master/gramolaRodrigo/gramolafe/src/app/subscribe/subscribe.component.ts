import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';

declare var Stripe: any;

@Component({
  selector: 'app-subscribe',
  standalone: true,
  imports: [CommonModule],
  template: `
  <div class="card">
    <h3>Elige tu suscripción</h3>
    <p *ngIf="isActive" class="alert alert-success">Ya estás suscrito.</p>
    <ul *ngIf="!isActive">
      <li *ngFor="let p of plans">
        <strong>{{ p.name }}</strong> — {{ (p.amountInCents/100) | number:'1.2-2' }} €
        <button class="btn btn-primary" (click)="startCheckout(p.id)" *ngIf="!isActive">Suscribirme</button>
      </li>
    </ul>
    <div *ngIf="!isActive" id="payment-element"></div>
    <button *ngIf="clientSecret && !isActive" class="btn btn-success" (click)="confirm()">Pagar ahora</button>
    <p class="message" *ngIf="message">{{ message }}</p>
  </div>
  `,
})
export class SubscribeComponent implements OnInit {
  plans: any[] = [];
  message = '';
  stripe: any;
  elements: any;
  clientSecret: string | null = null;
  isActive = false;

  constructor(private http: HttpClient, private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.stripe = Stripe('pk_test_51SGRYtLYhxLLPwjrdBNI8h9ON82IcZ3vgt4lRa1rx9U9vMEyC9cLjeLvKB3IUHzzfUvoR55YvXHGNKhlrCIO5A7P00cvJSZ47X');
    this.http.get<any[]>(`${API_URL}/plans`).subscribe({
      next: d => this.plans = d,
      error: () => this.message = 'No se pudieron cargar los planes'
    });
    // Opcional: podríamos consultar el estado para ocultar botones aquí si tuviéramos el bar en sesión
    const bar = this.auth.currentUserValue;
    if (bar) {
      this.http.get<any>(`${API_URL}/bars/subscription-status`, { params: { barId: bar.id } }).subscribe(s => {
        this.isActive = !!s.active;
        if (this.isActive) {
          this.message = 'Ya estás suscrito.';
          // Redirige fuera de la página de suscripción para que "se vaya esa zona"
          setTimeout(() => this.router.navigateByUrl('/'), 500);
        }
      });
    }
  }

  startCheckout(planId: string) {
    this.message='';
    this.http.post(`${API_URL}/payments/subscription-intent`, { planId }).subscribe({
      next: (res: any) => {
        this.clientSecret = res.clientSecret;
        const options = { clientSecret: this.clientSecret, appearance: { theme: 'stripe' } };
        this.elements = this.stripe.elements(options);
        const paymentElement = this.elements.create('payment');
        paymentElement.mount('#payment-element');
        // Guardamos plan elegido para confirmar después
        (this as any)._selectedPlanId = planId;
      },
      error: () => this.message = 'No se pudo iniciar el pago'
    });
  }

  async confirm() {
    this.message = '';
    const { error, paymentIntent } = await this.stripe.confirmPayment({ elements: this.elements, redirect: 'if_required' });
    if (error) { this.message = error.message || 'Error al confirmar el pago'; return; }
    if (!paymentIntent) { this.message = 'No se recibió estado de pago.'; return; }
    if (paymentIntent.status === 'requires_action' || paymentIntent.status === 'processing') {
      this.message = 'Procesando autenticación...';
      return;
    }
    if (paymentIntent.status !== 'succeeded') {
      this.message = 'Pago no completado.';
      return;
    }
    // Emitir evento global para reanudar audio que podría haber quedado bloqueado por el flujo de pago
    try { window.dispatchEvent(new Event('app:resume-audio')); } catch(e) {}
    const bar = this.auth.currentUserValue;
    if (!bar) { this.message = 'Inicia sesión para confirmar la suscripción.'; return; }
    const planId = (this as any)._selectedPlanId;
    this.http.post(`${API_URL}/payments/subscription-confirm`, { planId, barId: bar.id }).subscribe({
      next: (res: any) => {
        this.message = 'Suscripción activada. ¡Gracias!';
        // Si el backend devuelve el bar, guardamos sesión automáticamente
        if (res?.bar) {
          try { this.auth.login(res.bar); } catch {}
        }
        this.isActive = true;
        this.clientSecret = null;
        try { window.dispatchEvent(new Event('app:subscription-updated')); } catch(e) {}
        setTimeout(() => this.router.navigateByUrl('/'), 900);
      },
      error: () => this.message = 'No se pudo confirmar la suscripción'
    });
  }
}
