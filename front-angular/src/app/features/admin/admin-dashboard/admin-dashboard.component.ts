import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { EventService } from '../../../core/services/event.service';
import { UserService } from '../../../core/services/user.service';
import { PaymentService } from '../../../core/services/payment.service';
import { forkJoin } from 'rxjs';

interface DashboardStats {
  // Event stats
  totalEvents: number;
  activeEvents: number;
  upcomingEvents: number;
  totalTicketsSold: number;
  occupancyRate: number;
  
  // Payment stats
  totalPayments: number;
  approvedPayments: number;
  totalRevenue: number;
  approvedRevenue: number;
  approvalRate: number;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private eventService = inject(EventService);
  private userService = inject(UserService);
  private paymentService = inject(PaymentService);
  private router = inject(Router);

  currentUser = this.authService.getCurrentUser();
  isLoading = true;
  
  stats: DashboardStats = {
    totalEvents: 0,
    activeEvents: 0,
    upcomingEvents: 0,
    totalTicketsSold: 0,
    occupancyRate: 0,
    totalPayments: 0,
    approvedPayments: 0,
    totalRevenue: 0,
    approvedRevenue: 0,
    approvalRate: 0
  };

  quickActions = [
    {
      title: 'Gestionar Eventos',
      description: 'Crear, editar y eliminar eventos',
      icon: 'bi-calendar-event',
      route: '/admin/events',
      color: 'primary'
    },
    {
      title: 'Gestionar Consumos',
      description: 'Administrar consumos y categorías',
      icon: 'bi-cup-hot-fill',
      route: '/admin/consumptions',
      color: 'danger'
    },
    {
      title: 'Gestionar Empleados',
      description: 'Crear y administrar empleados',
      icon: 'bi-people',
      route: '/admin/employees',
      color: 'success'
    },
    {
      title: 'Analíticas',
      description: 'Ver estadísticas y reportes',
      icon: 'bi-graph-up',
      route: '/admin/analytics',
      color: 'info'
    }
  ];

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;

    // Cargar estadísticas de eventos y pagos simultáneamente
    forkJoin({
      eventStats: this.eventService.getEventStats(),
      paymentStats: this.paymentService.getPaymentStats()
    }).subscribe({
      next: (result) => {
        // Asignar estadísticas de eventos
        if (result.eventStats) {
          this.stats.totalEvents = result.eventStats.totalEvents || 0;
          this.stats.activeEvents = result.eventStats.activeEvents || 0;
          this.stats.upcomingEvents = result.eventStats.upcomingEvents || 0;
          this.stats.totalTicketsSold = result.eventStats.totalTicketsSold || 0;
          this.stats.occupancyRate = result.eventStats.occupancyRate || 0;
        }

        // Asignar estadísticas de pagos
        if (result.paymentStats) {
          this.stats.totalPayments = result.paymentStats.totalPayments || 0;
          this.stats.approvedPayments = result.paymentStats.approvedPayments || 0;
          this.stats.totalRevenue = result.paymentStats.totalRevenue || 0;
          this.stats.approvedRevenue = result.paymentStats.approvedRevenue || 0;
          this.stats.approvalRate = result.paymentStats.approvalRate || 0;
        }

        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error al cargar estadísticas del dashboard:', error);
        this.isLoading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }
}
