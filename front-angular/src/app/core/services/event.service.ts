import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event, EventCategory, ConsumptionCategory, Consumption } from '../../shared/models/event.model';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class EventService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = environment.eventsServiceUrl;

  // Event CRUD
  getEvents(): Observable<Event[]> {
    const user = this.authService.getCurrentUser();
    
    // Si es ADMIN, traer solo sus eventos
    if (user?.role === 'ADMIN') {
      return this.http.get<Event[]>(`${this.apiUrl}/event-service/event/my-events`);
    }
    
    // Si es CUSTOMER o público, traer todos los eventos
    return this.http.get<Event[]>(`${this.apiUrl}/event-service/event`);
  }

  getEventById(id: number): Observable<Event> {
    return this.http.get<Event>(`${this.apiUrl}/event-service/event/${id}`);
  }

  createEvent(event: Event): Observable<Event> {
    return this.http.post<Event>(`${this.apiUrl}/event-service/event`, event);
  }

  updateEvent(id: number, event: Event): Observable<Event> {
    return this.http.put<Event>(`${this.apiUrl}/event-service/event/${id}`, event);
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/event-service/event/${id}`);
  }

  deleteEventLogical(id: number): Observable<Event> {
    return this.http.delete<Event>(`${this.apiUrl}/event-service/event/logical/${id}`);
  }

  // Event Categories
  getEventCategories(): Observable<EventCategory[]> {
    return this.http.get<EventCategory[]>(`${this.apiUrl}/event-service/category`);
  }

  getActiveEventCategories(): Observable<EventCategory[]> {
    return this.http.get<EventCategory[]>(`${this.apiUrl}/event-service/category/active`);
  }

  createEventCategory(category: EventCategory): Observable<EventCategory> {
    return this.http.post<EventCategory>(`${this.apiUrl}/event-service/category`, category);
  }

  updateEventCategory(id: number, category: EventCategory): Observable<EventCategory> {
    return this.http.put<EventCategory>(`${this.apiUrl}/event-service/category/${id}`, category);
  }

  deleteEventCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/event-service/category/${id}`);
  }

  toggleEventCategoryStatus(id: number): Observable<EventCategory> {
    return this.http.put<EventCategory>(`${this.apiUrl}/event-service/category/status/${id}`, {});
  }

  // Consumption Categories
  getConsumptionCategories(): Observable<ConsumptionCategory[]> {
    return this.http.get<ConsumptionCategory[]>(`${this.apiUrl}/event-service/consumption-category`);
  }

  getActiveConsumptionCategories(): Observable<ConsumptionCategory[]> {
    return this.http.get<ConsumptionCategory[]>(`${this.apiUrl}/event-service/consumption-category/active`);
  }

  createConsumptionCategory(category: ConsumptionCategory): Observable<ConsumptionCategory> {
    return this.http.post<ConsumptionCategory>(`${this.apiUrl}/event-service/consumption-category`, category);
  }

  updateConsumptionCategory(id: number, category: ConsumptionCategory): Observable<ConsumptionCategory> {
    return this.http.put<ConsumptionCategory>(`${this.apiUrl}/event-service/consumption-category/${id}`, category);
  }

  deleteConsumptionCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/event-service/consumption-category/${id}`);
  }

  toggleConsumptionCategoryStatus(id: number): Observable<ConsumptionCategory> {
    return this.http.put<ConsumptionCategory>(`${this.apiUrl}/event-service/consumption-category/status/${id}`, {});
  }

  // Consumptions CRUD
  getConsumptions(): Observable<Consumption[]> {
    return this.http.get<Consumption[]>(`${this.apiUrl}/event-service/consumption`);
  }

  getConsumptionById(id: number): Observable<Consumption> {
    return this.http.get<Consumption>(`${this.apiUrl}/event-service/consumption/${id}`);
  }

  createConsumption(consumption: Consumption): Observable<Consumption> {
    return this.http.post<Consumption>(`${this.apiUrl}/event-service/consumption`, consumption);
  }

  updateConsumption(id: number, consumption: Consumption): Observable<Consumption> {
    return this.http.put<Consumption>(`${this.apiUrl}/event-service/consumption/${id}`, consumption);
  }

  deleteConsumption(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/event-service/consumption/${id}`);
  }

  // Event Image Upload
  uploadEventImage(eventId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('image', file);
    
    return this.http.post(`${this.apiUrl}/event-service/event/${eventId}/image`, formData);
  }

  getEventImageUrl(eventId: number): string {
    return `${this.apiUrl}/event-service/event/${eventId}/image`;
  }
  
  // Statistics
  getEventStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/event-service/event/stats`);
  }
}
