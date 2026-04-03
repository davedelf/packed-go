import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import Swal from 'sweetalert2';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email-required',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './verify-email-required.component.html',
  styleUrls: ['./verify-email-required.component.css']
})
export class VerifyEmailRequiredComponent implements OnInit {
  email: string = '';
  resending: boolean = false;
  resendSuccess: boolean = false;
  resendError: string = '';
  countdown: number = 60;
  canResend: boolean = false;
  private countdownInterval: any;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (!user) {
      this.router.navigate(['/auth/customer/login']);
      return;
    }
    
    if (user.isEmailVerified) {
      // Si ya está verificado, redirigir al dashboard correspondiente
      this.redirectToDashboard(user.role);
      return;
    }

    this.email = user.email;
    this.startCountdown();
  }

  startCountdown(): void {
    this.canResend = false;
    this.countdown = 60;
    
    this.countdownInterval = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        this.canResend = true;
        clearInterval(this.countdownInterval);
      }
    }, 1000);
  }

  resendVerificationEmail(): void {
    if (!this.canResend || this.resending) return;

    this.resending = true;
    this.resendError = '';
    this.resendSuccess = false;

    if (!this.email) {
      this.resendError = 'No se pudo obtener el email del usuario.';
      this.resending = false;
      return;
    }

    this.authService.resendVerificationEmail(this.email).subscribe({
      next: () => {
        this.resendSuccess = true;
        this.resending = false;
        this.startCountdown();
        
        // Ocultar mensaje de éxito después de 5 segundos
        setTimeout(() => {
          this.resendSuccess = false;
        }, 5000);
      },
      error: (error) => {
        console.error('Error resending verification email:', error);
        this.resendError = error.error?.message || 'Error al enviar el correo de verificación. Por favor, intenta nuevamente.';
        this.resending = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/customer/login']);
  }

  checkVerificationStatus(): void {
    const user = this.authService.getCurrentUser();
    if (user?.isEmailVerified) {
      this.redirectToDashboard(user.role);
    } else {
      Swal.fire('No verificado', 'Tu correo aún no ha sido verificado. Por favor, revisa tu bandeja de entrada.', 'info');
    }
  }

  private redirectToDashboard(role: string): void {
    if (role === 'CUSTOMER') {
      this.router.navigate(['/customer/dashboard']);
    } else if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.router.navigate(['/']);
    }
  }

  ngOnDestroy(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }
}
