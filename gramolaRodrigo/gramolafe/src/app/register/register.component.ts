import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../user.service';

@Component({
  selector: 'app-register',
  standalone: true, // <-- ESTA LÍNEA ES LA CLAVE
  imports: [CommonModule, FormsModule],
  // the project uses `register.html` and `register.css` in the same folder
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  name?: string;
  email?: string;
  pwd?: string;
  pwd2?: string;
  clientId?: string;
  clientSecret?: string;
  message?: string;

  constructor(private userService: UserService) { }

  registrar() {
    this.message = '';
    if (this.pwd !== this.pwd2) {
      this.message = 'Error: Las contraseñas no coinciden';
      return;
    }
  const userData = { name: this.name, email: this.email, pwd1: this.pwd, pwd2: this.pwd2, clientId: this.clientId || null, clientSecret: this.clientSecret || null };

    this.userService.register(userData).subscribe({
      next: () => {
        this.message = `Registro correcto. Revisa tu correo para confirmar la cuenta.`;
      },
      error: (error: any) => {
        if (error?.status === 409) {
          this.message = 'Error: Ya existe un bar activo con ese email o nombre.';
        } else if (error?.status === 406) {
          this.message = 'Error: Datos inválidos (revisa contraseñas y campos obligatorios).';
        } else {
          this.message = 'Ha ocurrido un error al registrar el bar.';
        }
      }
    });
  }
}