import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.5rem; color: #b0b0b0; }
    .input-glass { 
      width: 100%; padding: 10px; 
      background: rgba(255,255,255,0.1); 
      border: 1px solid rgba(255,255,255,0.2); 
      color: white; border-radius: 8px; font-size: 1rem;
    }
    .row { display: flex; gap: 10px; }
    .half { flex: 1; }
    .actions { display: flex; justify-content: space-between; margin-top: 1.5rem; gap: 1rem; }
    .btn-pay {
      background: linear-gradient(45deg, #bb86fc, #7c4dff); color: white; border: none;
      padding: 10px 20px; border-radius: 50px; cursor: pointer; font-weight: bold; width: 100%;
    }
    .btn-cancel {
      background: transparent; border: 1px solid #cf6679; color: #cf6679;
      padding: 10px 20px; border-radius: 50px; cursor: pointer; font-weight: bold; width: 100%;
    }
    @keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
  `]
})
export class PaymentFormComponent {
  @Input() songTitle: string = '';
  @Input() price: number = 0.99;
  @Output() cancel = new EventEmitter<void>();
  @Output() pay = new EventEmitter<void>();

  // Datos del formulario (simulados)
  cardNumber: string = '';
  expiry: string = '';
  cvc: string = '';

  onCancel() {
    this.cancel.emit();
  }
  
  onPay() {
    // Aquí iría la validación real o llamada a Stripe
    if (!this.cardNumber || !this.expiry || !this.cvc) {
      alert('Por favor, rellena los datos de pago (simulados).');
      return;
    }
    this.pay.emit();
  }
}