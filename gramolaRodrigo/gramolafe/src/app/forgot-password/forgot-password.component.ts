import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="card">
    <h3>Recuperar contraseña</h3>
    <div class="form-row">
      <label>Email</label>
      <input type="email" [(ngModel)]="email" placeholder="bar@ejemplo.com" />
    </div>
    <div class="actions">
      <button class="btn btn-primary" (click)="submit()">Enviar</button>
    </div>
    <p class="message" *ngIf="message">{{ message }}</p>
  </div>
  `,
})
export class ForgotPasswordComponent {
  email = '';
  message = '';

  constructor(private http: HttpClient) {}

  submit() {
    this.message = '';
    if (!this.email.trim()) { this.message = 'Introduce un email'; return; }
    this.http.post(`${API_URL}/bars/forgot-password`, { email: this.email }).subscribe({
      next: () => this.message = 'Si el email existe, se ha enviado un correo con instrucciones. Revisa también los logs del backend en esta demo.',
      error: err => {
        const msg = err?.error || err?.message || 'Error al solicitar recuperación';
        this.message = `Error: ${msg}`;
      }
    });
  }
}
