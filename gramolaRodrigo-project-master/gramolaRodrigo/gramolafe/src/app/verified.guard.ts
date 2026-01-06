import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable, map, of } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class VerifiedGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): Observable<boolean | UrlTree> {
    // Verifica si el usuario tiene suscripción activa
    return this.authService.checkSubscriptionStatus().pipe(
      map(isActive => {
        if (isActive) {
          return true; // Puede pasar
        } else {
          // Si no ha pagado, redirigir a la página de suscripción
          return this.router.createUrlTree(['/subscribe']);
        }
      })
    );
  }
}