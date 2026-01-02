import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user.service';
import { AuthService } from '../auth.service'; // <-- 1. IMPORTAR AuthService

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  // Variables para el formulario
  email?: string;
  pwd?: string;
  message?: string;

  // 2. INYECTAR AuthService
  constructor(private userService: UserService, private authService: AuthService) { }

  iniciarSesion() {
    this.message = '';
    const credentials = {
      email: this.email,
      pwd: this.pwd
    };

    this.userService.login(credentials).subscribe({
      next: (response: any) => {
        // 3. EN LUGAR DE MOSTRAR UN MENSAJE, LLAMAMOS AL AuthService
        this.authService.login(response);
        // El mensaje de bienvenida ahora lo mostrará el componente principal
      },
      error: (error: any) => {
        // --- CÓDIGO CORREGIDO ---
        // Extraemos el mensaje de error real
        if (error?.error && typeof error.error === 'string') {
          this.message = `Error: ${error.error}`;
        } else if (error?.error && error.error.message) {
          this.message = `Error: ${error.error.message}`;
        } else if (error?.message) {
          this.message = `Error: ${error.message}`;
        } else {
          this.message = 'Ha ocurrido un error al iniciar sesión.';
        }
        console.error('Error en el login', error);
      }
    });
  }
}