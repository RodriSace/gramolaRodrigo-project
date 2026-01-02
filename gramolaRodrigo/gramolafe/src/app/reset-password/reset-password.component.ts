import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="card">
    <h3>Restablecer contraseña</h3>
    <div class="form-row">
      <label>Token</label>
      <input type="text" [(ngModel)]="token" placeholder="pega aquí el token del email" />
    </div>
    <div class="form-row">
      <label>Nueva contraseña</label>
      <input type="password" [(ngModel)]="newPassword" />
    </div>
    <div class="actions">
      <button class="btn btn-primary" (click)="submit()">Cambiar contraseña</button>
    </div>
    <p class="message" *ngIf="message">{{ message }}</p>
  </div>
  `,
})
export class ResetPasswordComponent {
  token = '';
  newPassword = '';
  message = '';

  constructor(private http: HttpClient) {}

  submit() {
    this.message = '';
    if (!this.token.trim() || !this.newPassword.trim()) { this.message = 'Token y nueva contraseña requeridos'; return; }
    this.http.post(`${API_URL}/bars/reset-password`, { token: this.token, newPassword: this.newPassword }).subscribe({
      next: () => this.message = 'Contraseña actualizada. Ya puedes iniciar sesión.',
      error: err => {
        const msg = err?.error || err?.message || 'Error al resetear';
        this.message = `Error: ${msg}`;
      }
    });
  }
}
