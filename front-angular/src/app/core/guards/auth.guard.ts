import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Siempre verificar que el usuario esté autenticado
  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to customer login page, preservando la URL de retorno
  router.navigate(['/customer/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
