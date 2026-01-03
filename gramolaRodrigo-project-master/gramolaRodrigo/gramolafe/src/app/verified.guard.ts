import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const verifiedGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUserValue;
  if (user && user.verified) {
    return true;
  }
  // Si no está verificado, lo llevamos a confirmación (o al inicio)
  router.navigateByUrl('/confirm-email');
  return false;
};
