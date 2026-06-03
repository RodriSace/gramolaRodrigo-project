import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../services/user.service';

/**
 * COMPONENTE DE CONFIRMACIÓN DE CUENTA (ConfirmComponent)
 * 
 * ¿Qué es y para qué sirve?
 * Es un Standalone Component (Componente Autónomo) introducido en Angular 17+ que no necesita
 * declararse en un NgModule. Se encarga de procesar la verificación de correo electrónico.
 * 
 * Flujo Explicado para Defensa:
 * 1. El barman recibe un correo del MailService con un enlace conteniendo un token único 
 *    (ej: http://127.0.0.1:4200/confirm?token=UUID-UNICO).
 * 2. Al hacer clic, Angular intercepta esta ruta y carga este componente.
 * 3. En 'ngOnInit', el componente lee el token de la URL, lo envía al backend para verificarlo.
 * 4. Si el token es correcto, muestra un mensaje de éxito y redirige automáticamente
 *    a la pantalla de elección de planes de suscripción (/subscription).
 */
@Component({
  selector: 'app-confirm',
  standalone: true, // Indica que es un componente autónomo y moderno
  imports: [CommonModule], // Permite usar directivas estructurales clásicas como *ngIf
  template: `
    <div class="confirm-container">
        <div class="confirm-card">
            <!-- Spinner animado de carga -->
            <div class="loader" *ngIf="loading"></div>
            <!-- Título de confirmación exitosa -->
            <h1 *ngIf="!loading && success">¡Cuenta Confirmada!</h1>
            <!-- Título de error -->
            <h1 *ngIf="!loading && !success">Token inválido</h1>
            <!-- Mensaje de estado dinámico -->
            <p>{{ message }}</p>
        </div>
    </div>
  `,
  styles: [`
    .confirm-container { height: 100vh; display: flex; justify-content: center; align-items: center; background: #0d0d12; color: white; font-family: 'Inter', sans-serif; }
    .confirm-card { background: rgba(255,255,255,0.05); padding: 3rem; border-radius: 24px; text-align: center; border: 1px solid rgba(255,255,255,0.1); width: 400px; }
    .loader { border: 4px solid #f3f3f3; border-top: 4px solid #2575fc; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; margin: 0 auto 1rem; }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    h1 { color: #1db954; }
  `]
})
export class ConfirmComponent implements OnInit {
  // Banderas lógicas para gestionar la UI reactivamente
  loading = true;
  success = false;
  message = 'Estamos verificando tu cuenta...';

  // Inyectamos ActivatedRoute para leer parámetros de la URL,
  // el UserService para llamar a la API y el Router para la navegación entre páginas.
  constructor(
    private route: ActivatedRoute,
    private userService: UserService,
    private router: Router
  ) {}

  // ========================================================
  // 📧 FLUJO 2: CONFIRMACIÓN DE CORREO — PASO 4 (Frontend Confirm Component)
  // Lee el token de la barra de direcciones (?token=UUID) y llama a userService.confirm(token)
  // ========================================================
  /**
   * Método del ciclo de vida de Angular (OnInit).
   * Se ejecuta una única vez en cuanto el componente se dibuja en pantalla.
   */
  ngOnInit() {
    // 1. Extraemos de forma síncrona el parámetro 'token' de la URL actual
    const token = this.route.snapshot.queryParamMap.get('token');
    
    if (token) {
      // 2. Disparamos la petición HTTP asíncrona de confirmación al backend
      this.userService.confirm(token).subscribe({
        next: (res) => {
          this.loading = false;
          this.success = true;
          this.message = 'Redirigiendo a los planes de suscripción...';
          
          // Redirige al barman a elegir su suscripción tras 2 segundos de cortesía
          setTimeout(() => this.router.navigate(['/subscription']), 2000);
        },
        error: (err) => {
          this.loading = false;
          this.success = false;
          this.message = 'El enlace ha expirado o es incorrecto.';
        }
      });
    } else {
      this.loading = false;
      this.message = 'No se ha proporcionado un token de confirmación.';
    }
  }
}
