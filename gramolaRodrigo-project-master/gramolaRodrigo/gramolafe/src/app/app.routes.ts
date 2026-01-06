import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { QueueComponent } from './queue/queue.component';
import { SongSearchComponent } from './song-search/song-search.component';
import { SubscribeComponent } from './subscribe/subscribe.component';
import { VerifiedGuard } from './verified.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  
  // Rutas Públicas
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  
  // Ruta de Suscripción (Accesible tras login)
  { path: 'subscribe', component: SubscribeComponent },
  
  // Rutas Protegidas (Requieren Haber Pagado)
  { 
    path: 'search', 
    component: SongSearchComponent,
    canActivate: [VerifiedGuard] // Solo entra si pagó
  },
  { 
    path: 'queue', 
    component: QueueComponent,
    canActivate: [VerifiedGuard] // Solo entra si pagó
  },

  { path: '**', redirectTo: 'login' }
];