import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SubscribeComponent } from './subscribe/subscribe.component';
import { VerifiedGuard } from './verified.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Rutas Públicas
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  // Ruta de Suscripción (Accesible tras login)
  { path: 'subscribe', component: SubscribeComponent },

  // Dashboard unificado (Requiere Haber Pagado)
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [VerifiedGuard] // Solo entra si pagó
  },

  // Redirecciones para mantener compatibilidad
  { path: 'search', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'queue', redirectTo: 'dashboard', pathMatch: 'full' },

  { path: '**', redirectTo: 'login' }
];
