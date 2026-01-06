import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email: string = '';
  password: string = '';
  isLoading = false;

  constructor(private authService: AuthService, private router: Router) {}

  login() {
    if (!this.email || !this.password) {
      alert('Completa todos los campos');
      return;
    }

    this.isLoading = true;

    this.authService.login({ email: this.email, pwd: this.password }).subscribe({
      next: (user: any) => {
        console.log('Login correcto, verificando suscripción...');
        
        // --- AQUÍ ESTÁ LA CLAVE DEL FLUJO ---
        this.authService.checkSubscriptionStatus().subscribe(isActive => {
          this.isLoading = false;
          if (isActive) {
            // Si ya pagó, va a buscar música
            this.router.navigate(['/search']);
          } else {
            // Si NO ha pagado, va a suscribirse obligatoriamente
            this.router.navigate(['/subscribe']);
          }
        });
      },
      error: (err: any) => {
        this.isLoading = false;
        console.error(err);
        alert('Credenciales incorrectas');
      }
    });
  }
}