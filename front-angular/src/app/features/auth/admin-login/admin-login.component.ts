import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-login',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './admin-login.component.html',
  styleUrl: './admin-login.component.css'
})
export class AdminLoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loginForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  showPassword = false;
  showResendEmailButton = false;
  resendingEmail = false;
  resendSuccess = false;
  emailVerifiedSuccess = false;

  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Verificar si viene desde verificación de email
    this.route.queryParams.subscribe(params => {
      if (params['emailVerified'] === 'true') {
        this.emailVerifiedSuccess = true;
        // Ocultar mensaje después de 5 segundos
        setTimeout(() => {
          this.emailVerifiedSuccess = false;
        }, 5000);
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.showResendEmailButton = false;
    this.resendSuccess = false;

    this.authService.adminLogin(this.loginForm.value).subscribe({
      next: (response) => {
        // Verificar si el email está verificado
        if (response.user.isEmailVerified === false) {
          this.errorMessage = 'Tu correo electrónico aún no ha sido verificado. Por favor, revisa tu bandeja de entrada.';
          this.showResendEmailButton = true;
          this.isLoading = false;
          return;
        }
        
        this.router.navigate(['/admin/dashboard']);
      },
      error: (error) => {
        console.error('Error en login:', error);
        this.errorMessage = error.error?.message || 'Credenciales inválidas. Por favor, intenta nuevamente.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  resendVerificationEmail(): void {
    this.resendingEmail = true;
    this.resendSuccess = false;
    this.errorMessage = '';

    const email = this.loginForm.get('email')?.value;
    if (!email) {
      this.errorMessage = 'Por favor, ingresa tu correo electrónico.';
      this.resendingEmail = false;
      return;
    }

    this.authService.resendVerificationEmail(email).subscribe({
      next: () => {
        this.resendSuccess = true;
        this.resendingEmail = false;
        this.errorMessage = '✓ Correo de verificación enviado exitosamente. Revisa tu bandeja de entrada.';
        this.showResendEmailButton = false;
        
        // Ocultar mensaje de éxito después de 5 segundos
        setTimeout(() => {
          this.errorMessage = '';
        }, 5000);
      },
      error: (error) => {
        console.error('Error al reenviar correo:', error);
        this.errorMessage = error.error?.message || 'Error al enviar el correo. Por favor, intenta nuevamente.';
        this.resendingEmail = false;
      }
    });
  }

  goToLanding(): void {
    this.router.navigate(['/']);
  }

  // Getters para validación en template
  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }
}
