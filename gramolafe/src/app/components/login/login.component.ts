import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SpotiService } from '../../services/spoti.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink], // Módulos para directivas, formularios y rutas
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  // Datos del formulario de inicio de sesión
  email = '';
  password = '';
  loading = false;
  errorMessage = '';

  constructor(
    private router: Router, // Para navegación entre componentes
    private http: HttpClient, // Cliente para peticiones HTTP
    private spoti: SpotiService // Servicio para interactuar con Spotify
  ) {}

  // ========================================================
  // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 1
  // onLogin() hace POST /users/login para verificar credenciales y estados (confirmed y subscriptionActive)
  // ========================================================
  /**
   * Gestiona el inicio de sesión del usuario.
   */
  onLogin(event: Event) {
    event.preventDefault(); // Detiene el comportamiento de envío nativo
    this.loading = true;
    this.errorMessage = '';

    const credentials = { email: this.email, password: this.password };

    // Envío de credenciales al backend
    this.http.post('http://127.0.0.1:8080/users/login', credentials).subscribe({
      next: (user: any) => {
        // Almacenar datos del barman y configurar el cliente de Spotify
        this.spoti.clientId = user.spotifyClientId;
        this.spoti.email = user.email;
        
        if (user.spotifyAccessToken) {
          user.spotifyConnected = true;
          this.spoti.spotiToken = user.spotifyAccessToken;
          
          // Almacena temporalmente los tokens de Spotify
          sessionStorage.setItem('spotiToken', user.spotifyAccessToken);
          localStorage.setItem('spotiToken', user.spotifyAccessToken);
        } else {
          user.spotifyConnected = false;
        }
        
        localStorage.setItem('user', JSON.stringify(user));
        
        // Redirección en función de si Spotify ya está vinculado
        if (user.spotifyConnected) {
          this.router.navigate(['/music']);
        } else {
          this.getToken();
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Email o contraseña incorrectos, o cuenta no confirmada/pagada.';
      }
    });
  }

  // Redirección al portal de autorización de Spotify (OAuth2)
  private getToken() {
    this.spoti.redirectToSpotify();
  }

  // ========================================================
  // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 1
  // onForgotPassword() hace POST /users/forgot-password enviando el email ingresado en el modal
  // ========================================================
  // Control del modal de recuperación de contraseña
  showResetModal = false;
  resetEmail = '';
  resetLoading = false;
  resetSuccessMessage = '';
  resetErrorMessage = '';

  closeResetModal() {
    this.showResetModal = false;
    this.resetEmail = '';
    this.resetSuccessMessage = '';
    this.resetErrorMessage = '';
  }

  /**
   * Envía la solicitud de recuperación de contraseña.
   */
  onForgotPassword() {
    this.resetLoading = true;
    this.resetSuccessMessage = '';
    this.resetErrorMessage = '';

    this.http.post('http://127.0.0.1:8080/users/forgot-password', {
        email: this.resetEmail
    }).subscribe({
        next: (res: any) => {
            this.resetSuccessMessage = '¡Enlace enviado! Revisa tu correo de Mailtrap.';
            this.resetLoading = false;
        },
        error: (err: any) => {
            this.resetErrorMessage = err.error?.message || 'Error al solicitar recuperación.';
            this.resetLoading = false;
        }
    });
  }
}
