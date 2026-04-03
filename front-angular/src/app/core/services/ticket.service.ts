import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface TicketConsumptionDetail {
  id: number;
  consumptionId: number;
  consumptionName: string;
  quantity: number;
  priceAtPurchase: number;
  active: boolean;
  redeem: boolean;
}

export interface TicketConsumption {
  id: number;
  redeem: boolean;
  ticketDetails: TicketConsumptionDetail[];
}

export interface Ticket {
  ticketId: number;
  userId: number;
  passCode: string;
  passId: number;
  qrCode?: string; // Base64 PNG image
  eventId: number;
  eventName: string;
  eventDate: string;
  eventLocation: string;
  eventLocationName?: string; // Nombre del lugar
  eventLat?: number; // Latitud para Google Maps
  eventLng?: number; // Longitud para Google Maps
  active: boolean;
  redeemed: boolean;
  createdAt: string;
  purchasedAt: string;
  redeemedAt: string | null;
  ticketConsumption: TicketConsumption;
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private apiUrl = `${environment.eventsServiceUrl}/event-service/tickets`;

  /**
   * Obtiene todos los tickets de un usuario
   */
  getUserTickets(userId: number): Observable<Ticket[]> {
    const headers = this.getHeaders();
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}`, { headers }).pipe(
      catchError(error => {
        console.error('❌ Error obteniendo tickets:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Obtiene solo los tickets activos de un usuario
   */
  getActiveTickets(userId: number): Observable<Ticket[]> {
    const headers = this.getHeaders();
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}/active`, { headers }).pipe(
      catchError(error => {
        console.error('❌ Error obteniendo tickets activos:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Obtiene solo los tickets canjeados de un usuario
   */
  getRedeemedTickets(userId: number): Observable<Ticket[]> {
    const headers = this.getHeaders();
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}/redeemed`, { headers }).pipe(
      catchError(error => {
        console.error('❌ Error obteniendo tickets canjeados:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Genera los headers con el token JWT
   */
  private getHeaders(): HttpHeaders {
    const token = this.authService.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Download QR code as image with ticket code
   */
  downloadQRCode(qrCode: string, fileName: string, passCode?: string): void {
    if (!qrCode) {
      console.error('❌ No QR code available');
      return;
    }

    // Si no hay passCode, descargar el QR original
    if (!passCode) {
      const link = document.createElement('a');
      link.href = qrCode;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      return;
    }

    // Crear canvas para combinar QR con información del código
    const img = new Image();
    img.onload = () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      
      if (!ctx) {
        console.error('❌ No se pudo obtener el contexto del canvas');
        return;
      }

      // Dimensiones del canvas
      const padding = 40;
      const textHeight = 60;
      canvas.width = img.width + (padding * 2);
      canvas.height = img.height + textHeight + (padding * 2);

      // Fondo blanco
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Dibujar QR code centrado
      ctx.drawImage(img, padding, padding);

      // Configurar texto
      ctx.fillStyle = '#333333';
      ctx.textAlign = 'center';
      
      // Texto "Código:"
      ctx.font = 'bold 18px Arial';
      ctx.fillText('Código:', canvas.width / 2, img.height + padding + 25);
      
      // Últimos 8 caracteres del código
      const last8Chars = passCode.slice(-8);
      ctx.font = 'bold 24px monospace';
      ctx.fillText(last8Chars, canvas.width / 2, img.height + padding + 50);

      // Convertir canvas a blob y descargar
      canvas.toBlob((blob) => {
        if (blob) {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = fileName;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(url);
        }
      }, 'image/png');
    };

    img.onerror = () => {
      console.error('❌ Error cargando imagen QR');
    };

    img.src = qrCode;
  }

  /**
   * Download all QR codes from a list of tickets
   */
  downloadAllQRCodes(tickets: Ticket[]): void {
    tickets.forEach((ticket, index) => {
      if (ticket.qrCode) {
        setTimeout(() => {
          this.downloadQRCode(
            ticket.qrCode!,
            `ticket-${ticket.ticketId}-${ticket.eventName.replace(/\s/g, '-')}.png`,
            ticket.passCode
          );
        }, index * 500); // Delay between downloads to avoid browser blocking
      }
    });
  }
}
