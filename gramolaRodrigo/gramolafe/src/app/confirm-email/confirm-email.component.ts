import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-confirm-email',
  standalone: true,
  imports: [CommonModule],
  template: `
  <div class="card">
    <h3>Confirmación de email</h3>
    <p class="message">{{ message }}</p>
    <button *ngIf="canResend && email" (click)="resend()">Reenviar enlace a {{ email }}</button>
  </div>
  `,
})
export class ConfirmEmailComponent implements OnInit {
  message = 'Confirmando...';
  canResend = false;
  email: string | null = null;
  constructor(private http: HttpClient, private router: Router, private auth: AuthService) {}

  ngOnInit(): void {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    if (!token) { this.message = 'Token no proporcionado.'; return; }
    this.http.get<any>(`${API_URL}/bars/confirm-email`, { params: { token } }).subscribe({
      next: (bar) => {
        // Guardamos el usuario en sesión para que el guard de /subscribe deje pasar
        this.auth.login(bar);
        this.message = 'Email verificado. Redirigiendo a suscripción...';
        setTimeout(() => this.router.navigateByUrl('/subscribe'), 1200);
      },
      error: () => {
        // Consultamos el estado para dar un mensaje más claro
        this.http.get<any>(`${API_URL}/bars/confirm-email/status`, { params: { token } }).subscribe(status => {
          if (status.status === 'not-found') {
            this.message = 'Token inválido. ¿Quizá ya generaste uno nuevo?';
          } else if (status.expired) {
            this.message = 'El enlace ha expirado. Puedes reenviarlo si indicas tu email.';
            this.canResend = true;
          } else if (status.used) {
            if (status.barVerified) {
              this.message = 'Ya estabas verificado. Redirigiendo a suscripción...';
              setTimeout(() => this.router.navigateByUrl('/subscribe'), 1000);
            } else {
              this.message = 'Token ya usado.';
            }
          } else {
            this.message = 'No se pudo confirmar el email.';
          }
        }, () => this.message = 'Token inválido o expirado.')
      }
    });
  }

  resend() {
    if (!this.email) {
      this.email = prompt('Introduce tu email para reenviar el enlace de verificación:');
    }
    if (!this.email) return;
    this.http.post(`${API_URL}/bars/resend-verification`, { email: this.email }, { responseType: 'text' }).subscribe({
      next: () => this.message = 'Enlace reenviado (revisa logs del backend si no tienes SMTP).',
      error: () => this.message = 'No se pudo reenviar el enlace.'
    });
  }
}
