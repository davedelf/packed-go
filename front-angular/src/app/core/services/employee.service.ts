import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// Interfaces
export interface Employee {
  id: number;
  email: string;
  username: string;
  document: number;
  assignedEventIds: number[];
  createdAt: string;
  isActive: boolean;
}

export interface EmployeeWithEvents {
  id: number;
  email: string;
  username: string;
  document: number;
  assignedEvents: AssignedEvent[];
  createdAt: string;
  isActive: boolean;
}

export interface AssignedEvent {
  id: number;
  name: string;
  location?: string;
  eventDate?: string;
  status?: string;
}

export interface CreateEmployeeRequest {
  email: string;
  username: string;
  password: string;
  document: number;
  assignedEventIds: number[];
}

export interface UpdateEmployeeRequest {
  email: string;
  username: string;
  document: number;
  assignedEventIds: number[];
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.usersServiceUrl}/admin/employees`;

  /**
   * Obtiene los headers con el token JWT
   */
  private getHeaders(): { headers: { Authorization: string } } {
    const token = localStorage.getItem('token');
    return {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    };
  }

  /**
   * Obtiene todos los empleados del admin actual (con sus eventos asignados)
   */
  getMyEmployees(): Observable<EmployeeWithEvents[]> {
    return this.http.get<ApiResponse<EmployeeWithEvents[]>>(this.apiUrl, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Obtiene un empleado específico por ID
   */
  getEmployeeById(id: number): Observable<EmployeeWithEvents> {
    return this.http.get<ApiResponse<EmployeeWithEvents>>(`${this.apiUrl}/${id}`, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Crea un nuevo empleado
   */
  createEmployee(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<ApiResponse<Employee>>(this.apiUrl, request, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Actualiza un empleado existente
   */
  updateEmployee(id: number, request: UpdateEmployeeRequest): Observable<Employee> {
    return this.http.put<ApiResponse<Employee>>(`${this.apiUrl}/${id}`, request, this.getHeaders())
      .pipe(map(response => response.data));
  }

  /**
   * Cambia el estado activo/inactivo de un empleado
   */
  toggleEmployeeStatus(id: number): Observable<void> {
    return this.http.patch<ApiResponse<void>>(`${this.apiUrl}/${id}/toggle-status`, {}, this.getHeaders())
      .pipe(map(() => undefined));
  }

  /**
   * Elimina un empleado (soft delete - marca como inactivo)
   */
  deleteEmployee(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`, this.getHeaders())
      .pipe(map(() => undefined));
  }

  /**
   * Obtiene empleados asignados a un evento específico
   */
  getEmployeesByEvent(eventId: number): Observable<EmployeeWithEvents[]> {
    return this.http.get<ApiResponse<EmployeeWithEvents[]>>(
      `${this.apiUrl}/by-event/${eventId}`,
      this.getHeaders()
    ).pipe(map(response => response.data));
  }

  // ========================================
  // MÉTODOS PARA DASHBOARD DE EMPLEADOS
  // ========================================

  private employeeApiUrl = `${environment.usersServiceUrl}/employee`;

  /**
   * Obtiene los eventos asignados al empleado actual
   */
  getAssignedEvents(): Observable<AssignedEvent[]> {
    return this.http.get<ApiResponse<AssignedEvent[]>>(
      `${this.employeeApiUrl}/assigned-events`,
      this.getHeaders()
    ).pipe(map(response => response.data));
  }

  /**
   * Valida un ticket de entrada escaneando el QR
   */
  validateTicket(qrCode: string, eventId: number): Observable<TicketValidationResponse> {
    return this.http.post<ApiResponse<TicketValidationResponse>>(
      `${this.employeeApiUrl}/validate-ticket`,
      { qrCode, eventId },
      this.getHeaders()
    ).pipe(map(response => response.data));
  }

  /**
   * Obtiene las consumiciones disponibles de un ticket
   */
  getTicketConsumptions(ticketConsumptionId: number): Observable<ConsumptionDetail[]> {
    return this.http.get<ConsumptionDetail[]>(
      `${environment.eventServiceUrl}/event-service/ticket-consumption/${ticketConsumptionId}/details`
    );
  }

  /**
   * Obtiene las consumiciones disponibles de un ticket de entrada
   */
  getTicketConsumptionsByTicket(ticketId: number): Observable<ConsumptionDetail[]> {
    return this.http.get<ConsumptionDetail[]>(
      `${environment.eventServiceUrl}/event-service/ticket-consumption/by-ticket/${ticketId}/details`
    );
  }

  /**
   * Registra/canjea una consumición
   */
  registerConsumption(request: RegisterConsumptionRequest): Observable<ConsumptionResponse> {
    return this.http.post<ApiResponse<ConsumptionResponse>>(
      `${this.employeeApiUrl}/register-consumption`,
      request,
      this.getHeaders()
    ).pipe(map(response => response.data));
  }

  /**
   * Obtiene estadísticas del empleado
   */
  getStats(): Observable<EmployeeStats> {
    return this.http.get<ApiResponse<EmployeeStats>>(
      `${this.employeeApiUrl}/stats`,
      this.getHeaders()
    ).pipe(map(response => response.data));
  }

  /**
   * Busca un ticket por los últimos 8 dígitos del código
   */
  findTicketByCode(code: string, eventId: number): Observable<TicketSearchResponse> {
    return this.http.post<ApiResponse<TicketSearchResponse>>(
      `${this.employeeApiUrl}/find-ticket-by-code`,
      { code, eventId }
    ).pipe(map(response => response.data));
  }
}

// ========================================
// INTERFACES ADICIONALES
// ========================================

export interface TicketValidationResponse {
  valid: boolean;
  message: string;
  ticketInfo?: {
    ticketId: number;
    userId: number;
    customerName: string;
    eventName: string;
    passType: string;
    alreadyUsed: boolean;
  };
}

export interface ConsumptionDetail {
  id: number;
  ticketId?: number;
  consumptionId: number;
  consumptionName: string;
  quantity: number;
  priceAtPurchase: number;
  active: boolean;
  redeem: boolean;
}

export interface RegisterConsumptionRequest {
  qrCode: string;
  eventId: number;
  detailId: number;
  quantity?: number;
}

export interface ConsumptionResponse {
  success: boolean;
  message: string;
  consumptionInfo?: {
    detailId: number;
    consumptionId: number;
    consumptionName: string;
    consumptionType: string;
    quantityRedeemed: number;
    remainingQuantity: number;
    fullyRedeemed: boolean;
    eventName: string;
  };
}

export interface EmployeeStats {
  ticketsScannedToday: number;
  consumptionsToday: number;
  totalScannedToday: number;
}

export interface TicketSearchResponse {
  ticketId: number;
  qrCode: string;
  passCode: string;
  eventId: number;
  eventName: string;
  userId: number;
}
