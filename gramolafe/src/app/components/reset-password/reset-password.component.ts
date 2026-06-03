import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

/**
 * COMPONENTE DE RESTABLECIMIENTO DE CONTRASEÑA (ResetPasswordComponent)
 * 
 * ¿Qué es y para qué sirve?
 * Este componente autónomo gestiona el formulario donde el barman introduce su nueva
 * contraseña tras haber hecho clic en el correo de recuperación.
 * 
 * Flujo de Recuperación para la Defensa:
 * 1. El barman pulsa "Olvidé mi contraseña" en el Login. El backend genera un token temporal,
 *    lo guarda en su fila en MySQL y le envía un email conteniendo un enlace con dicho token.
 * 2. Al pulsar el enlace, Angular carga este componente.
 * 3. Leemos el parámetro 'token' de los queryParams de la URL en el 'ngOnInit'.
 * 4. Al rellenar el formulario de nueva contraseña, enviamos un POST al backend con el token y
 *    la contraseña nueva.
 * 5. El backend desencripta y actualiza la contraseña de forma segura en MySQL si el token coincide y no ha caducado.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {
  // Variables de estado del formulario y mensajes
  token: string = '';
  newPassword = '';
  loading = false;
  successMessage = '';
  errorMessage = '';

  // Inyectamos ActivatedRoute para leer la URL, el Router para navegación y HttpClient para la llamada REST
  constructor(private route: ActivatedRoute, private router: Router, private http: HttpClient) {}

  // ========================================================
  // 🔑 FLUJO 8: "HE OLVIDADO MI CONTRASEÑA" — PASO 4
  // Captura el token de recuperación de la URL en ngOnInit y envía la contraseña nueva en onReset
  // ========================================================
  /**
   * Método de ciclo de vida.
   * Nos suscribimos a los parámetros de consulta de la URL (?token=xxxx) para extraer la llave de restablecimiento.
   */
  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      if (!this.token) {
        this.errorMessage = 'Enlace inválido. Vuelve a solicitar la recuperación.';
      }
    });
  }

  /**
   * Acción del botón de confirmación del formulario de contraseña.
   * Envía la nueva contraseña de forma asíncrona al backend para ser encriptada y guardada.
   */
  onReset(event: Event) {
    event.preventDefault(); // Evitamos que la página se refresque
    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Validamos la presencia del token
    if (!this.token) {
        this.errorMessage = 'No hay token de recuperación válido.';
        this.loading = false;
        return;
    }

    // Validación básica de seguridad frontend: longitud de la contraseña
    if (this.newPassword.length < 6) {
        this.errorMessage = 'La contraseña debe tener al menos 6 caracteres.';
        this.loading = false;
        return;
    }

    // Disparamos la petición POST de reseteo enviando el token y la nueva contraseña
    this.http.post('http://127.0.0.1:8080/users/reset-password', {
        token: this.token,
        newPassword: this.newPassword
    }).subscribe({
        next: (res: any) => {
            this.successMessage = res.message || 'Contraseña actualizada con éxito.';
            this.loading = false;
            // Si todo sale bien, podemos invitar al usuario a ir al login
        },
        error: (err) => {
            this.loading = false;
            this.errorMessage = err.error?.message || 'Error al restablecer la contraseña.';
        }
    });
  }
}

