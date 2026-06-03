import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { UserService } from '../../services/user.service';

/**
 * Componente para el registro de nuevos locales (bares).
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  // Datos del formulario de registro
  barName = '';
  email = '';
  pwd1 = '';
  pwd2 = '';
  clientId = '';
  clientSecret = '';
  loading = false; // Indica si se está procesando la petición

  constructor(
    private userService: UserService, // Servicio de gestión de usuarios
    private router: Router // Enrutador para navegación
  ) {}

  // ========================================================
  // 📝 FLUJO 1: UN BARMAN SE REGISTRA — PASO 2
  // Método del Componente que empaqueta y envía los datos al servicio Angular
  // ========================================================
  /**
   * Envía los datos de registro al backend.
   */
  onRegister(event: Event) {
    event.preventDefault(); // Evita el comportamiento de submit por defecto
    this.loading = true;
    
    const info = {
      bar: this.barName,
      email: this.email,
      pwd1: this.pwd1,
      pwd2: this.pwd2,
      clientId: this.clientId,
      clientSecret: this.clientSecret
    };

    // Llamada HTTP para registrar al usuario
    this.userService.register(info).subscribe({
      next: () => {
        alert('Registro exitoso. Revisa tu email para confirmar la cuenta y proceder al pago.');
        this.loading = false;
        this.router.navigate(['/login']); // Redirección al login
      },
      error: (err) => {
        // Manejo de errores HTTP
        if (err.status === 406) {
          alert('Error: Las contraseñas no coinciden o los datos son inválidos.');
        } else if (err.status === 409) {
          alert('Error: El bar ya existe en el sistema.');
        } else {
          alert('Error al registrar: ' + (err.error?.message || 'Servidor no disponible'));
        }
        this.loading = false;
      }
    });
  }
}
