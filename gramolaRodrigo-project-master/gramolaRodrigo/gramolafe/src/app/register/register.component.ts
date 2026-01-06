import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html'
})
export class RegisterComponent {
  user = {
    name: '',
    email: '',
    pwd1: '',
    pwd2: ''
  };

  constructor(private authService: AuthService, private router: Router) {}

  register() {
    if (!this.user.name || !this.user.email || !this.user.pwd1) {
      alert('Todos los campos son obligatorios.');
      return;
    }

    if (this.user.pwd1 !== this.user.pwd2) {
      alert('Las contraseñas no coinciden.');
      return;
    }

    this.authService.register(this.user).subscribe({
      next: () => {
        // MENSAJE ACTUALIZADO
        alert('¡Cuenta creada! Se ha enviado un enlace de verificación a tu correo (o revisa los logs de la consola Java).');
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        console.error('Error en registro:', err);
        if (err.status === 409) {
          alert('Error: El email o nombre de bar ya está registrado.');
        } else {
          alert('Ocurrió un error durante el registro.');
        }
      }
    });
  }
}