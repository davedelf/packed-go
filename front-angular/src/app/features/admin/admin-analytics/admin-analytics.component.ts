import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AnalyticsService, DashboardDTO } from '../../../core/services/analytics.service';

@Component({
  selector: 'app-admin-analytics',
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-analytics.component.html',
  styleUrl: './admin-analytics.component.css'
})
export class AdminAnalyticsComponent implements OnInit, OnDestroy {
  private analyticsService = inject(AnalyticsService);
  private router = inject(Router);

  dashboard: DashboardDTO | null = null;
  isLoading = true;
  error: string | null = null;
  autoRefreshInterval: any;
  Math = Math; // Para usar Math en el template

  ngOnInit(): void {
    this.loadDashboard();
    
    // Auto-refresh cada 5 minutos
    this.autoRefreshInterval = setInterval(() => {
      this.loadDashboard(true);
    }, 300000);
  }

  ngOnDestroy(): void {
    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
    }
  }

  loadDashboard(silent: boolean = false): void {
    if (!silent) {
      this.isLoading = true;
    }
    this.error = null;

    this.analyticsService.getDashboard().subscribe({
      next: (data: DashboardDTO) => {
        this.dashboard = data;
        this.isLoading = false;
      },
      error: (err: Error) => {
        this.error = err.message;
        this.isLoading = false;
        console.error('Error al cargar dashboard:', err);
      }
    });
  }

  refreshDashboard(): void {
    this.loadDashboard();
  }

  formatCurrency(value: number | null | undefined): string {
    if (value == null) return '$0.00';
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS'
    }).format(value);
  }

  formatPercentage(value: number | null | undefined): string {
    if (value == null) return '0.00%';
    return `${value.toFixed(2)}%`;
  }

  formatDate(dateString: string | null | undefined): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('es-AR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getGrowthClass(rate: number | null | undefined): string {
    if (rate == null) return 'text-secondary';
    if (rate > 0) return 'text-success';
    if (rate < 0) return 'text-danger';
    return 'text-secondary';
  }

  getGrowthIcon(rate: number | null | undefined): string {
    if (rate == null) return 'bi-dash';
    if (rate > 0) return 'bi-arrow-up';
    if (rate < 0) return 'bi-arrow-down';
    return 'bi-dash';
  }

  formatNumber(value: number | null | undefined, decimals: number = 2): string {
    if (value == null) return '0.00';
    return value.toFixed(decimals);
  }
}
