import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { employeeGuard } from './core/guards/employee.guard';
import { emailVerifiedGuard } from './core/guards/email-verified.guard';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';

export const routes: Routes = [
  // Landing page
  {
    path: '',
    loadComponent: () => import('./features/landing/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'home',
    redirectTo: '',
    pathMatch: 'full'
  },
  
  // Public event exploration (no auth required)
  {
    path: 'events',
    loadComponent: () => import('./features/events-explore/events-explore.component').then(m => m.EventsExploreComponent)
  },
  
  // Terms and Privacy
  {
    path: 'terms',
    loadComponent: () => import('./features/terms/terms.component').then(m => m.TermsComponent)
  },
  {
    path: 'privacy',
    loadComponent: () => import('./features/terms/terms.component').then(m => m.TermsComponent)
  },
  
  // Auth routes - Admin
  {
    path: 'admin/login',
    loadComponent: () => import('./features/auth/admin-login/admin-login.component').then(m => m.AdminLoginComponent)
  },
  {
    path: 'admin/register',
    loadComponent: () => import('./features/auth/admin-register/admin-register.component').then(m => m.AdminRegisterComponent)
  },
  
  // Auth routes - Customer
  {
    path: 'customer/login',
    loadComponent: () => import('./features/auth/customer-login/customer-login.component').then(m => m.CustomerLoginComponent)
  },
  {
    path: 'customer/register',
    loadComponent: () => import('./features/auth/customer-register/customer-register.component').then(m => m.CustomerRegisterComponent)
  },
  
  // Auth routes - Employee
  {
    path: 'employee/login',
    loadComponent: () => import('./features/auth/employee-login/employee-login.component').then(m => m.EmployeeLoginComponent)
  },
  
  // Email verification required
  {
    path: 'auth/verify-email-required',
    loadComponent: () => import('./features/auth/verify-email-required/verify-email-required.component').then(m => m.VerifyEmailRequiredComponent)
  },
  
  // Email verification (from email link)
  {
    path: 'verify-email',
    loadComponent: () => import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent)
  },
  
  // Admin routes (protected + email verified)
  {
    path: 'admin/dashboard',
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  {
    path: 'admin/events',
    loadComponent: () => import('./features/admin/events-management/events-management.component').then(m => m.EventsManagementComponent),
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  {
    path: 'admin/consumptions',
    loadComponent: () => import('./features/admin/consumptions-management/consumptions-management.component').then(m => m.ConsumptionsManagementComponent),
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  {
    path: 'admin/analytics',
    loadComponent: () => import('./features/admin/admin-analytics/admin-analytics.component').then(m => m.AdminAnalyticsComponent),
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  {
    path: 'admin/employees',
    loadComponent: () => import('./features/admin/employee-management/employee-management.component').then(m => m.EmployeeManagementComponent),
    canActivate: [adminGuard, emailVerifiedGuard]
  },
  
  // Customer routes (protected + email verified)
  {
    path: 'customer/dashboard',
    loadComponent: () => import('./features/customer/customer-dashboard/customer-dashboard.component').then(m => m.CustomerDashboardComponent),
    canActivate: [authGuard, emailVerifiedGuard]
  },
  {
    path: 'customer/events/:id',
    loadComponent: () => import('./features/customer/event-detail/event-detail.component').then(m => m.EventDetailComponent),
    canActivate: [authGuard, emailVerifiedGuard]
  },
  {
    path: 'customer/checkout',
    loadComponent: () => import('./features/customer/checkout/checkout.component').then(m => m.CheckoutComponent),
    canActivate: [authGuard, emailVerifiedGuard]
  },
  {
    path: 'customer/orders/success',
    loadComponent: () => import('./features/customer/order-success/order-success.component').then(m => m.OrderSuccessComponent),
    canActivate: [authGuard, emailVerifiedGuard]
  },
  
  // Employee routes (protected + email verified)
  {
    path: 'employee/dashboard',
    loadComponent: () => import('./features/employee/employee-dashboard/employee-dashboard.component').then(m => m.EmployeeDashboardComponent),
    canActivate: [employeeGuard, emailVerifiedGuard]
  },
  
  // Wildcard route
  { path: '**', redirectTo: '/admin/login' }
];

