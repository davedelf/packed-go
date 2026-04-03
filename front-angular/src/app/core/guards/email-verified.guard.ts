import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const emailVerifiedGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.getCurrentUser();
  
  if (!user) {
    // Si no hay usuario, redirigir al login
    router.navigate(['/auth/customer/login']);
    return false;
  }

  if (user.isEmailVerified === false) {
    // Si el email no está verificado, redirigir a la página de verificación
    router.navigate(['/auth/verify-email-required']);
    return false;
  }

  return true;
};
