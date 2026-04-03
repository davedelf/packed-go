import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { 
  MultiOrderCheckoutResponse, 
  Order, 
  MultiOrderSession,
  SessionStateResponse
} from '../../shared/models/order.model';

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private apiUrl = environment.ordersServiceUrl;

  /**
   * Obtiene los headers con el token JWT
   */
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Realiza el checkout para un admin específico
   * @param adminId ID del admin
   * @returns Observable con la URL de pago
   */
  checkoutSingleAdmin(adminId: number): Observable<{ paymentUrl: string, preferenceId: string }> {
    // Detectar timezone del navegador del usuario
    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    
    return this.http.post<{ paymentUrl: string, preferenceId: string }>(
      `${this.apiUrl}/orders/checkout/single`,
      { adminId, timezone },
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene todas las órdenes del usuario actual
   * @returns Observable con el array de órdenes
   */
  getUserOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(
      `${this.apiUrl}/orders/user`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene una orden específica por ID
   * @param orderId ID de la orden
   * @returns Observable con los detalles de la orden
   */
  getOrderById(orderId: number): Observable<Order> {
    return this.http.get<Order>(
      `${this.apiUrl}/orders/${orderId}`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene una orden específica por número de orden
   * @param orderNumber Número de orden (ej: ORD-202511-1763834635718)
   * @returns Observable con los detalles de la orden
   */
  getOrderByNumber(orderNumber: string): Observable<Order> {
    return this.http.get<Order>(
      `${this.apiUrl}/orders/${orderNumber}`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene el estado de una sesión de checkout
   * @param sessionId ID de la sesión
   */
  getSessionStatus(sessionId: string): Observable<SessionStateResponse> {
    return this.http.get<SessionStateResponse>(
      `${this.apiUrl}/orders/checkout/session/${sessionId}`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene el estado actual del checkout (si existe)
   */
  getCurrentCheckoutState(): Observable<SessionStateResponse> {
    return this.http.get<SessionStateResponse>(
      `${this.apiUrl}/orders/checkout/current`,
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Abandona la sesión actual
   */
  abandonSession(sessionId: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/orders/checkout/session/${sessionId}/abandon`,
      {},
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Maneja errores HTTP
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Ocurrió un error desconocido';
    
    if (error.error instanceof ErrorEvent) {
      // Error del lado del cliente
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Error del lado del servidor
      switch (error.status) {
        case 400:
          errorMessage = error.error?.message || 'Datos inválidos';
          break;
        case 401:
          errorMessage = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
          localStorage.removeItem('token');
          localStorage.removeItem('userRole');
          window.location.href = '/customer/login';
          break;
        case 404:
          errorMessage = error.error?.message || 'Carrito no encontrado';
          break;
        case 409:
          errorMessage = error.error?.message || 'Conflicto al procesar la orden';
          break;
        case 410:
          errorMessage = error.error?.message || 'El carrito ha expirado';
          break;
        case 500:
          errorMessage = error.error?.message || 'Error interno del servidor';
          break;
        default:
          errorMessage = error.error?.message || `Error del servidor: ${error.status}`;
      }
    }
    
    console.error('OrderService Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }
}
