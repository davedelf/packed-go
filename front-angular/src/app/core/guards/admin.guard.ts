import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true;
  }

  // Redirect to customer dashboard or login
  if (authService.isAuthenticated()) {
    router.navigate(['/customer/dashboard']);
  } else {
    router.navigate(['/admin/login']);
  }
  return false;
};
