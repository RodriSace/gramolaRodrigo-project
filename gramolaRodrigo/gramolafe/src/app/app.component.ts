import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // <-- 1. IMPORTAR CommonModule
// RouterOutlet removed because app template doesn't use routing outlet
import { RegisterComponent } from './register/register.component';
import { LoginComponent } from './login/login.component';
import { SongSearchComponent } from './song-search/song-search.component'; // <-- 1. IMPORTAMOS EL NUEVO COMPONENTE
import { QueueComponent } from './queue/queue.component';
import { AuthService } from './auth.service'; // <-- 2. IMPORTAR AuthService
import { Observable } from 'rxjs'; // <-- 3. IMPORTAR Observable
import { RouterOutlet, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { API_URL } from './api.config';

@Component({
  selector: 'app-root',
  standalone: true,
  // 2. AÑADIMOS TODOS LOS COMPONENTES QUE USAMOS EN EL HTML
  imports: [CommonModule, RegisterComponent, LoginComponent, SongSearchComponent, QueueComponent, RouterOutlet, RouterLink], 
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'gramolafe';
  currentUser$: Observable<any>; // <-- 5. CREAR VARIABLE
  subscriptionActive = false;

  // 6. INYECTAR AuthService
  constructor(private authService: AuthService, private http: HttpClient) {
    this.currentUser$ = this.authService.currentUser$;
    // Suscríbete a cambios de usuario para refrescar el estado de suscripción
    this.currentUser$.subscribe(user => {
      if (user?.id) {
        this.http.get<any>(`${API_URL}/bars/subscription-status`, { params: { barId: user.id } }).subscribe(s => {
          this.subscriptionActive = !!s.active;
        }, () => this.subscriptionActive = false);

        // Conexión automática a Deezer: si no hay token guardado, redirigir al login de Deezer
        // Previene bucle: sólo intentar una vez por sesión hasta conectarse
        const triedKey = `deezerTried:${user.id}`;
        const alreadyTried = sessionStorage.getItem(triedKey) === '1';
        if (!alreadyTried) {
          // Llamamos a /deezer/me; si 404, no hay token → redirigimos a /deezer/login
          this.http.get(`${API_URL}/deezer/me`, { params: { barId: user.id }, responseType: 'text' as 'json' }).subscribe({
            next: () => {
              // Ok: token presente; marcamos conectado y no volvemos a intentar
              sessionStorage.setItem(triedKey, '1');
            },
            error: (err) => {
              if (err?.status === 404) {
                sessionStorage.setItem(triedKey, '1');
                // Redirigir a flujo OAuth de Deezer (se abrirá en la misma pestaña)
                window.location.href = `${API_URL}/deezer/login?barId=${encodeURIComponent(user.id)}`;
              }
            }
          });
        }
      } else {
        this.subscriptionActive = false;
      }
    });

    // Escuchar actualizaciones de suscripción para refrescar el estado del header
    try {
      window.addEventListener('app:subscription-updated', () => {
        const user = (this.authService as any).currentUserValue;
        if (user?.id) {
          this.http.get<any>(`${API_URL}/bars/subscription-status`, { params: { barId: user.id } }).subscribe(s => {
            this.subscriptionActive = !!s.active;
          }, () => this.subscriptionActive = false);
        }
      });
    } catch(e) {}
  }

  // 7. AÑADIR MÉTODO PARA CERRAR SESIÓN
  logout() {
    this.authService.logout();
  }
}