import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { TicketService, Ticket } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { PaymentService } from '../../../core/services/payment.service';
import { MultiOrderCheckoutResponse, SessionStateResponse, SessionPaymentGroup } from '../../../shared/models/order.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './order-success.component.html',
  styleUrls: ['./order-success.component.css']
})
export class OrderSuccessComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private orderService = inject(OrderService);
  private ticketService = inject(TicketService);
  private authService = inject(AuthService);
  private paymentService = inject(PaymentService);

  sessionId: string = '';
  orderId: string = '';
  sessionData: SessionStateResponse | null = null;
  tickets: Ticket[] = [];
  expectedTicketCount: number = 0; // Cantidad esperada de tickets de la orden actual
  isLoading = true;
  isLoadingTickets = false;
  isVerifyingPayments = false;
  verificationMessage = '';

  ngOnInit(): void {
    // Obtener el sessionId y orderId de los query params
    this.route.queryParams.subscribe(params => {
      this.sessionId = params['sessionId'];
      this.orderId = params['orderId'];
      
      // Priorizar orderId si está disponible, ya que el sessionId de Stripe 
      // no es compatible con el endpoint de sesión de órdenes actual
      if (this.orderId) {
        this.loadSingleOrderData(this.orderId);
      } else if (this.sessionId) {
        this.loadSessionData();
      } else {
        this.router.navigate(['/customer/dashboard']);
      }
    });
  }

  loadSingleOrderData(orderId: string): void {
    this.isLoading = true;
    
    // Primero obtener los detalles de la orden para saber cuántos tickets esperar
    this.orderService.getOrderByNumber(orderId).subscribe({
      next: (order) => {
        // Calcular el total de tickets esperados (suma de cantidades de todos los items)
        this.expectedTicketCount = order.items?.reduce((sum, item) => sum + item.quantity, 0) || 0;
        
        // Ahora verificar el estado del pago antes de cargar tickets
        this.paymentService.verifyPaymentStatus(orderId).subscribe({
          next: (status) => {
            // Esperar un momento para asegurar que el backend procesó todo (generación de tickets)
            setTimeout(() => {
              this.loadTickets();
            }, 1500);
          },
          error: (error) => {
            console.warn('⚠️ No se pudo verificar el pago, intentando cargar tickets de todos modos:', error);
            this.loadTickets();
          }
        });
      },
      error: (error) => {
        console.error('❌ Error obteniendo detalles de la orden:', error);
        // Si no se puede obtener la orden, intentar cargar tickets de todos modos
        this.loadTickets();
      }
    });
  }

  loadSessionData(): void {
    this.orderService.getSessionStatus(this.sessionId).subscribe({
      next: (data) => {
        this.sessionData = data;
        this.isLoading = false;
        
        // Verificar si hay órdenes pendientes de pago
        const pendingOrders = data.paymentGroups?.filter(
          group => group.paymentStatus === 'PENDING_PAYMENT'
        ) || [];
        
        if (pendingOrders.length > 0) {
          this.verifyPendingPayments(pendingOrders);
        } else {
          // Si no hay pendientes, cargar tickets directamente
          this.loadTickets();
        }
      },
      error: (error) => {
        console.error('Error loading session:', error);
        this.isLoading = false;
      }
    });
  }

  verifyPendingPayments(orders: any[]): void {
    this.isVerifyingPayments = true;
    this.verificationMessage = 'Verificando estado de los pagos...';
    
    // Esperar 2 segundos antes de verificar (dar tiempo al webhook de Stripe)
    setTimeout(() => {
      const verifications = orders.map(order => 
        this.paymentService.verifyPaymentStatus(order.orderNumber)
      );
      
      forkJoin(verifications).subscribe({
        next: (results) => {
          this.isVerifyingPayments = false;
          this.verificationMessage = 'Pagos verificados exitosamente';
          
          // Recargar sesión para obtener estados actualizados
          setTimeout(() => {
            this.reloadSessionAndTickets();
          }, 1000);
        },
        error: (error) => {
          console.error('❌ Error verificando pagos:', error);
          this.isVerifyingPayments = false;
          this.verificationMessage = 'No se pudieron verificar todos los pagos';
          
          // Intentar cargar tickets de todos modos
          setTimeout(() => {
            this.loadTickets();
          }, 1000);
        }
      });
    }, 2000);
  }

  reloadSessionAndTickets(): void {
    this.orderService.getSessionStatus(this.sessionId).subscribe({
      next: (data) => {
        this.sessionData = data;
        this.loadTickets();
      },
      error: (error) => {
        console.error('Error reloading session:', error);
        this.loadTickets();
      }
    });
  }

  loadTickets(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      console.error('No user ID available');
      this.isLoading = false;
      return;
    }

    this.isLoadingTickets = true;
    this.ticketService.getActiveTickets(userId).subscribe({
      next: (tickets) => {
        // Si hay orderId y sabemos cuántos tickets esperar, tomar solo los N más recientes
        if (this.orderId && this.expectedTicketCount > 0) {
          // Ordenar por fecha de creación descendente
          const sortedTickets = [...tickets].sort((a, b) => 
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
          
          // Tomar solo la cantidad esperada de tickets más recientes
          this.tickets = sortedTickets.slice(0, this.expectedTicketCount);
        } else if (this.orderId) {
          // Fallback: si no sabemos cuántos esperar, usar filtro de tiempo
          const sortedTickets = [...tickets].sort((a, b) => 
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
          
          const oneMinuteAgo = Date.now() - (60 * 1000);
          this.tickets = sortedTickets.filter(ticket => 
            new Date(ticket.createdAt).getTime() > oneMinuteAgo
          );
        } else {
          // Si no hay orderId, mostrar todos los tickets (caso de sesión múltiple)
          this.tickets = tickets;
        }
        
        this.isLoadingTickets = false;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Error loading tickets:', error);
        this.isLoadingTickets = false;
        this.isLoading = false;
      }
    });
  }

  downloadTicketQR(ticket: Ticket): void {
    if (ticket.qrCode) {
      this.ticketService.downloadQRCode(
        ticket.qrCode,
        `ticket-${ticket.ticketId}-${ticket.eventName.replace(/\s/g, '-')}.png`,
        ticket.passCode
      );
    }
  }

  downloadAllQRCodes(): void {
    const ticketsWithQR = this.tickets.filter(t => t.qrCode);
    if (ticketsWithQR.length > 0) {
      this.ticketService.downloadAllQRCodes(ticketsWithQR);
    }
  }

  hasTicketsWithQR(): boolean {
    return this.tickets.some(t => t.qrCode);
  }

  goToDashboard(): void {
    this.router.navigate(['/customer/dashboard']);
  }

  goToTickets(): void {
    this.router.navigate(['/customer/dashboard'], { queryParams: { tab: 'tickets' } });
  }

  openTicketLocation(ticket: Ticket): void {
    if (ticket.eventLat && ticket.eventLng) {
      const url = `https://www.google.com/maps/search/?api=1&query=${ticket.eventLat},${ticket.eventLng}`;
      window.open(url, '_blank');
    } else if (ticket.eventLocationName) {
      const url = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(ticket.eventLocationName)}`;
      window.open(url, '_blank');
    }
  }
}
