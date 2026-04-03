import { Component, inject, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-customer-register',
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './customer-register.component.html',
  styleUrl: './customer-register.component.css'
})
export class CustomerRegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  @ViewChild('termsContent') termsContent?: ElementRef;

  registerForm: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  showPassword = false;
  showTermsModal = false;
  hasScrolledToBottom = false;

  constructor() {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      document: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(100)]],
      bornDate: ['', [Validators.required]],
      telephone: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      gender: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]]
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    // Convert string values to correct types
    const registerData = {
      ...this.registerForm.value,
      document: Number(this.registerForm.value.document),
      telephone: Number(this.registerForm.value.telephone)
    };

    this.authService.customerRegister(registerData).subscribe({
      next: (response) => {
        this.successMessage = 'Registro exitoso. Redirigiendo al login...';
        setTimeout(() => {
          this.router.navigate(['/customer/login']);
        }, 2000);
      },
      error: (error) => {
        console.error('Error en registro:', error);
        this.errorMessage = error.error?.message || 'Error al registrar. Por favor, intenta nuevamente.';
        this.isLoading = false;
      },
      complete: () => {
        this.isLoading = false;
      }
    });
  }

  openTermsModal(): void {
    this.showTermsModal = true;
    this.hasScrolledToBottom = false;
  }

  closeTermsModal(): void {
    this.showTermsModal = false;
  }

  onTermsScroll(event: any): void {
    const element = event.target;
    const atBottom = element.scrollHeight - element.scrollTop <= element.clientHeight + 50;
    if (atBottom && !this.hasScrolledToBottom) {
      this.hasScrolledToBottom = true;
    }
  }

  acceptTermsAndClose(): void {
    this.registerForm.patchValue({ acceptTerms: true });
    this.closeTermsModal();
  }

  goToTerms(): void {
    window.open('/terms', '_blank');
  }

  goToLanding(): void {
    this.router.navigate(['/']);
  }

  // Getters para validación en template
  get username() { return this.registerForm.get('username'); }
  get email() { return this.registerForm.get('email'); }
  get document() { return this.registerForm.get('document'); }
  get password() { return this.registerForm.get('password'); }
  get name() { return this.registerForm.get('name'); }
  get lastName() { return this.registerForm.get('lastName'); }
  get bornDate() { return this.registerForm.get('bornDate'); }
  get telephone() { return this.registerForm.get('telephone'); }
  get gender() { return this.registerForm.get('gender'); }
  get acceptTerms() { return this.registerForm.get('acceptTerms'); }
}
