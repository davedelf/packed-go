import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject, timer, EMPTY } from 'rxjs';
import { catchError, tap, map, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { 
  Cart, 
  AddToCartRequest, 
  UpdateCartItemRequest,
  CartErrorResponse 
} from '../../shared/models/cart.model';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private http = inject(HttpClient);
  private apiUrl = environment.ordersServiceUrl;
  
  // Constante: Máximo de entradas por persona
  public readonly MAX_TICKETS_PER_PERSON = 10;
  
  // Observable para el carrito actual
  private cartSubject = new BehaviorSubject<Cart | null>(null);
  public cart$ = this.cartSubject.asObservable();
  
  // Observable para el tiempo restante (en segundos)
  private timeRemainingSubject = new BehaviorSubject<number>(0);
  public timeRemaining$ = this.timeRemainingSubject.asObservable();
  
  // Timer subscription
  private timerSubscription: any = null;

  constructor() {
    // No cargamos el carrito automáticamente aquí
    // Los componentes deben llamar explícitamente a loadCart() cuando lo necesiten
  }

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
   * Carga el carrito actual del usuario desde el backend
   */
  loadCart(): void {
    this.http.get<Cart>(`${this.apiUrl}/cart`, { headers: this.getHeaders() })
      .pipe(
        catchError((error: HttpErrorResponse) => {
          if (error.status === 404 || error.status === 410) {
            // No hay carrito activo (404) o el carrito expiró (410)
            // Ambos casos son normales y no deben mostrar error al usuario
            this.cartSubject.next(null);
            this.stopTimer(true);
            // Retornar un observable vacío para no propagar el error
            return EMPTY;
          }
          return this.handleError(error);
        })
      )
      .subscribe({
        next: (cart) => {
          this.cartSubject.next(cart);
          this.startTimer(cart.expiresAt);
        },
        error: (err) => {
          // Solo se llega aquí si hay un error real (no 404 o 410)
          console.error('Error loading cart:', err);
        }
      });
  }

  /**
   * Obtiene el carrito actual (Observable)
   */
  getCart(): Observable<Cart> {
    return this.http.get<Cart>(`${this.apiUrl}/cart`, { headers: this.getHeaders() })
      .pipe(
        tap(cart => {
          this.cartSubject.next(cart);
          this.startTimer(cart.expiresAt);
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Obtiene el valor actual del carrito (snapshot)
   */
  getCurrentCart(): Cart | null {
    return this.cartSubject.value;
  }

  /**
   * Agrega un evento con consumos al carrito
   */
  addToCart(request: AddToCartRequest): Observable<Cart> {
    return this.http.post<Cart>(
      `${this.apiUrl}/cart/add`, 
      request, 
      { headers: this.getHeaders() }
    ).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        this.startTimer(cart.expiresAt);
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Elimina un item del carrito
   */
  removeCartItem(itemId: number): Observable<Cart | null> {
    return this.http.delete<Cart>(
      `${this.apiUrl}/cart/items/${itemId}`, 
      { 
        headers: this.getHeaders(),
        observe: 'response'
      }
    ).pipe(
      map(response => {
        if (response.status === 204) {
          // Carrito vacío, fue eliminado
          this.cartSubject.next(null);
          this.stopTimer(true); // Resetear porque el carrito fue vaciado
          return null;
        }
        const cart = response.body;
        this.cartSubject.next(cart);
        if (cart) {
          this.startTimer(cart.expiresAt);
        }
        return cart;
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Actualiza la cantidad de un item en el carrito
   */
  updateCartItemQuantity(itemId: number, quantity: number): Observable<Cart> {
    const request: UpdateCartItemRequest = { quantity };
    
    return this.http.put<Cart>(
      `${this.apiUrl}/cart/items/${itemId}`,
      request,
      { headers: this.getHeaders() }
    ).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        if (cart) {
          this.startTimer(cart.expiresAt);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Actualiza la cantidad de una consumición específica en un item del carrito
   */
  updateConsumptionQuantity(itemId: number, consumptionId: number, quantity: number): Observable<Cart> {
    return this.http.put<Cart>(
      `${this.apiUrl}/cart/items/${itemId}/consumptions/${consumptionId}`,
      { quantity },
      { headers: this.getHeaders() }
    ).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        if (cart) {
          this.startTimer(cart.expiresAt);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Agrega una nueva consumición a un item existente del carrito
   * Si la consumición ya existe, incrementa su cantidad
   */
  addConsumptionToItem(itemId: number, consumptionId: number, quantity: number): Observable<Cart> {
    return this.http.post<Cart>(
      `${this.apiUrl}/cart/items/${itemId}/consumptions`,
      { consumptionId, quantity },
      { headers: this.getHeaders() }
    ).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        if (cart) {
          this.startTimer(cart.expiresAt);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Elimina una consumición de un item del carrito
   */
  removeConsumptionFromItem(itemId: number, consumptionId: number): Observable<Cart> {
    return this.http.delete<Cart>(
      `${this.apiUrl}/cart/items/${itemId}/consumptions/${consumptionId}`,
      { headers: this.getHeaders() }
    ).pipe(
      tap(cart => {
        this.cartSubject.next(cart);
        if (cart) {
          this.startTimer(cart.expiresAt);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene la cantidad total de entradas en el carrito para un evento específico
   */
  getTicketCountForEvent(eventId: number): number {
    const cart = this.cartSubject.value;
    if (!cart) return 0;
    
    const item = cart.items.find(i => i.eventId === eventId);
    return item ? item.quantity : 0;
  }

  /**
   * Verifica si se puede agregar más entradas sin exceder el máximo
   */
  canAddTickets(eventId: number, additionalQuantity: number): boolean {
    const currentCount = this.getTicketCountForEvent(eventId);
    return (currentCount + additionalQuantity) <= this.MAX_TICKETS_PER_PERSON;
  }

  /**
   * Vacía completamente el carrito
   */
  clearCart(): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/cart`, 
      { headers: this.getHeaders() }
    ).pipe(
      tap(() => {
        this.cartSubject.next(null);
        this.stopTimer(true); // Resetear porque el usuario vació el carrito
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Inicia el timer del carrito basado en la fecha de expiración
   */
  private startTimer(expiresAt: string): void {
    this.stopTimer(); // Detener timer anterior si existe
    
    // El servidor envía la fecha en UTC sin 'Z', así que la agregamos para parsear correctamente
    const expirationDate = new Date(expiresAt.endsWith('Z') ? expiresAt : expiresAt + 'Z');
    const now = new Date();
    const timeRemainingMs = expirationDate.getTime() - now.getTime();
    
    if (timeRemainingMs <= 0) {
      // Ya expiró
      this.timeRemainingSubject.next(0);
      this.cartSubject.next(null);
      return;
    }
    
    // Convertir a segundos
    this.timeRemainingSubject.next(Math.floor(timeRemainingMs / 1000));
    
    // Actualizar cada segundo
    this.timerSubscription = timer(0, 1000).subscribe(() => {
      const now = new Date();
      const remaining = expirationDate.getTime() - now.getTime();
      const remainingSeconds = Math.floor(remaining / 1000);
      
      if (remainingSeconds <= 0) {
        this.timeRemainingSubject.next(0);
        this.stopTimer(true); // Pasar true porque realmente expiró
        this.cartSubject.next(null);
        // Opcional: mostrar notificación
        console.warn('Cart expired!');
      } else {
        this.timeRemainingSubject.next(remainingSeconds);
      }
    });
  }

  /**
   * Detiene el timer del carrito
   * @param resetTime Si es true, resetea el tiempo a 0 (por expiración real)
   */
  private stopTimer(resetTime: boolean = false): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
      this.timerSubscription = null;
    }
    // Solo resetear a 0 si realmente expiró, no al detener manualmente
    if (resetTime) {
      this.timeRemainingSubject.next(0);
    }
  }

  /**
   * Formatea el tiempo restante a MM:SS
   */
  formatTimeRemaining(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  /**
   * Obtiene el total del carrito actual
   */
  getCartTotal(): number {
    const cart = this.cartSubject.value;
    return cart ? cart.totalAmount : 0;
  }

  /**
   * Obtiene el número de items en el carrito
   */
  getCartItemCount(): number {
    const cart = this.cartSubject.value;
    return cart ? cart.itemCount : 0;
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
      const serverError = error.error as CartErrorResponse;
      
      switch (error.status) {
        case 400:
          errorMessage = serverError.message || 'Datos inválidos';
          break;
        case 401:
          // Token inválido o expirado
          errorMessage = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
          // Limpiar token y redirigir al login
          localStorage.removeItem('token');
          localStorage.removeItem('userRole');
          window.location.href = '/customer/login';
          break;
        case 404:
          errorMessage = serverError.message || 'Carrito no encontrado';
          break;
        case 409:
          errorMessage = serverError.message || 'No hay stock disponible';
          break;
        case 410:
          errorMessage = serverError.message || 'El carrito ha expirado';
          break;
        case 500:
          // Error 500 puede ser por token inválido también
          if (serverError.message && serverError.message.includes('Invalid token')) {
            errorMessage = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
            localStorage.removeItem('token');
            localStorage.removeItem('userRole');
            window.location.href = '/customer/login';
          } else {
            errorMessage = serverError.message || 'Error interno del servidor';
          }
          break;
        case 503:
          errorMessage = 'Servicio no disponible. Intenta más tarde';
          break;
        default:
          errorMessage = serverError.message || `Error del servidor: ${error.status}`;
      }
    }
    
    console.error('CartService Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Limpia el servicio al destruir (detiene timer)
   */
  ngOnDestroy(): void {
    this.stopTimer();
  }
}
