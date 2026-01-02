import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-form.component.html',
  styleUrl: './payment-form.component.css'
})
export class PaymentFormComponent implements OnInit {
  @Input() clientSecret: string = '';
  @Input() stripe: any; // Ahora recibe la instancia de Stripe del padre
  @Output() paymentSucceeded = new EventEmitter<string>(); // paymentIntentId
  elements: any;
  message: string | null = null;

  constructor() { }

  ngOnInit(): void {
    const options = { clientSecret: this.clientSecret, appearance: { theme: 'stripe' } };
    this.elements = this.stripe.elements(options);
    const paymentElement = this.elements.create('payment');
    paymentElement.mount('#song-payment-element');
  }

  async handleSubmit() {
    this.message = null;
    const { error, paymentIntent } = await this.stripe.confirmPayment({
      elements: this.elements,
      // Evita recargar/navegar, sólo redirige si Stripe lo requiere
      redirect: 'if_required',
    });
    if (error) {
      this.message = error.message;
      return;
    }
    if (paymentIntent && paymentIntent.status === 'succeeded') {
      this.message = 'Pago confirmado';
      this.paymentSucceeded.emit(paymentIntent.id);
    } else if (paymentIntent) {
      this.message = `Estado del pago: ${paymentIntent.status}`;
    } else {
      this.message = 'No se pudo confirmar el pago.';
    }
  }
}