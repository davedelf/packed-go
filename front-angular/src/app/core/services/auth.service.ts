import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, map } from 'rxjs';
import { Router } from '@angular/router';
import { 
  AdminLoginRequest, 
  CustomerLoginRequest, 
  LoginResponse, 
  AdminRegistrationRequest, 
  CustomerRegistrationRequest,
  AuthUser 
} from '../../shared/models/user.model';
import { environment } from '../../../environments/environment';

// Interfaz para la respuesta envuelta del backend
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  
  private currentUserSubject = new BehaviorSubject<AuthUser | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  private apiUrl = environment.authServiceUrl;

  constructor() {}

  private getUserFromStorage(): AuthUser | null {
    try {
      const userJson = localStorage.getItem('currentUser');
      if (!userJson || userJson === 'undefined' || userJson === 'null') {
        return null;
      }
      return JSON.parse(userJson);
    } catch (error) {
      console.error('Error al parsear usuario del localStorage:', error);
      localStorage.removeItem('currentUser');
      return null;
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    // Verificar si el token está expirado
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiration = payload.exp * 1000; // Convertir a milisegundos
      const now = Date.now();
      
      if (now >= expiration) {
        console.warn('Token expirado, limpiando sesión...');
        this.logout();
        return false;
      }
      
      return true;
    } catch (error) {
      console.error('Error al validar token:', error);
      this.logout();
      return false;
    }
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  getUserId(): number | null {
    const user = this.getCurrentUser();
    return user?.id || null;
  }

  // Admin Login
  adminLogin(credentials: AdminLoginRequest): Observable<LoginResponse> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/auth/admin/login`, credentials)
      .pipe(
        map(response => response.data),
        tap(loginData => {
          this.saveAuthData(loginData);
        })
      );
  }

  // Customer Login
  customerLogin(credentials: CustomerLoginRequest): Observable<LoginResponse> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/auth/customer/login`, credentials)
      .pipe(
        map(response => response.data),
        tap(loginData => {
          this.saveAuthData(loginData);
        })
      );
  }

  // Employee Login
  employeeLogin(credentials: { email: string; password: string }): Observable<LoginResponse> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.apiUrl}/auth/employee/login`, credentials)
      .pipe(
        map(response => response.data),
        tap(loginData => {
          this.saveAuthData(loginData);
        })
      );
  }

  // Admin Registration
  adminRegister(userData: AdminRegistrationRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/admin/register`, userData);
  }

  // Customer Registration
  customerRegister(userData: CustomerRegistrationRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/customer/register`, userData);
  }

  private saveAuthData(response: LoginResponse): void {
    localStorage.setItem('token', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('currentUser', JSON.stringify(response.user));
    localStorage.setItem('permissions', JSON.stringify(response.permissions));
    this.currentUserSubject.next(response.user);
  }

  logout(): void {
    const token = this.getToken();
    if (token) {
      // Opcional: llamar al endpoint de logout del backend
      this.http.post(`${this.apiUrl}/auth/logout`, { token }).subscribe();
    }
    
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('permissions');
    this.currentUserSubject.next(null);
    this.router.navigate(['/']); // Volver a la landing page
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'ADMIN';
  }

  isCustomer(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'CUSTOMER' || user?.role === 'USER';
  }

  isEmployee(): boolean {
    const user = this.getCurrentUser();
    return user?.role === 'EMPLOYEE';
  }

  getPermissions(): string[] {
    const permissionsJson = localStorage.getItem('permissions');
    return permissionsJson ? JSON.parse(permissionsJson) : [];
  }

  hasPermission(permission: string): boolean {
    const permissions = this.getPermissions();
    return permissions.includes(permission);
  }

  // Resend verification email
  resendVerificationEmail(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/resend-verification`, { email });
  }

  // Verify email with token (cuando el usuario hace clic en el enlace del correo)
  verifyEmail(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/auth/verify-email?token=${token}`);
  }

  // Change password for logged user
  changePassword(userId: number, currentPassword: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/change-password/${userId}`, {
      currentPassword,
      newPassword
    });
  }
}
