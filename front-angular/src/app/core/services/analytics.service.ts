import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

// Interfaces para los DTOs
export interface DashboardDTO {
  organizerId: number;
  organizerName: string;
  lastUpdated: string;
  salesMetrics: SalesMetricsDTO;
  eventMetrics: EventMetricsDTO;
  consumptionMetrics: ConsumptionMetricsDTO;
  revenueMetrics: RevenueMetricsDTO;
  topPerformers: TopPerformersDTO;
  trends: TrendsDTO;
}

export interface SalesMetricsDTO {
  totalTicketsSold: number;
  totalOrders: number;
  averageTicketsPerOrder: number;
  conversionRate: number;
}

export interface EventMetricsDTO {
  totalEvents: number;
  activeEvents: number;
  pastEvents: number;
  totalCapacity: number;
  averageOccupancyRate: number;
}

export interface ConsumptionMetricsDTO {
  totalConsumptions: number;
  activeConsumptions: number;
  totalConsumptionsSold: number;
  consumptionsRedeemed: number;
  consumptionsPending: number;
  redemptionRate: number;
  totalTicketsSold: number;
  ticketsRedeemed: number;
  ticketsPending: number;
  ticketRedemptionRate: number;
  mostSoldConsumptionId?: number;
  mostSoldConsumptionName?: string;
  mostSoldConsumptionQuantity?: number;
}

export interface RevenueMetricsDTO {
  totalRevenue: number;
  revenueToday: number;
  revenueThisWeek: number;
  revenueThisMonth: number;
  revenueFromTickets: number;
  revenueFromConsumptions: number;
  averageRevenuePerEvent: number;
  averageRevenuePerCustomer: number;
}

export interface TopPerformersDTO {
  topEvents: EventPerformanceDTO[];
  topConsumptions: ConsumptionPerformanceDTO[];
  topCategories: CategoryPerformanceDTO[];
}

export interface EventPerformanceDTO {
  eventId: number;
  eventName: string;
  ticketsSold: number;
  revenue: number;
  occupancyRate: number;
}

export interface ConsumptionPerformanceDTO {
  consumptionId: number;
  consumptionName: string;
  quantitySold: number;
  revenue: number;
  redemptionRate?: number;
}

export interface CategoryPerformanceDTO {
  categoryName: string;
  eventsCount: number;
  totalRevenue: number;
}

export interface TrendsDTO {
  dailyTrends: DailyTrendDTO[];
  monthlyTrends: MonthlyTrendDTO[];
}

export interface DailyTrendDTO {
  date: string;
  orders: number;
  revenue: number;
  ticketsSold: number;
}

export interface MonthlyTrendDTO {
  yearMonth: string;
  orders: number;
  revenue: number;
  ticketsSold: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.analyticsServiceUrl}/dashboard`;

  /**
   * Obtiene el dashboard del usuario autenticado
   */
  getDashboard(): Observable<DashboardDTO> {
    const headers = this.getHeaders();
    return this.http.get<DashboardDTO>(`${this.apiUrl}`, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Obtiene el dashboard de un organizador específico (solo SUPER_ADMIN)
   */
  getDashboardByOrganizer(organizerId: number): Observable<DashboardDTO> {
    const headers = this.getHeaders();
    return this.http.get<DashboardDTO>(`${this.apiUrl}/${organizerId}`, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Verifica el estado del servicio
   */
  healthCheck(): Observable<string> {
    return this.http.get(`${this.apiUrl}/health`, { responseType: 'text' }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Construye los headers con el token JWT
   */
  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Manejo de errores
   */
  private handleError(error: any): Observable<never> {
    let errorMessage = 'Ocurrió un error al obtener los datos de analytics';
    
    if (error.error instanceof ErrorEvent) {
      // Error del lado del cliente
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Error del lado del servidor
      errorMessage = `Error ${error.status}: ${error.error?.message || error.message}`;
    }
    
    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}
